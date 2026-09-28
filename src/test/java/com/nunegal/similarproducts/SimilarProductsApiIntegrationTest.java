package com.nunegal.similarproducts;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.serviceUnavailable;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.nunegal.similarproducts.domain.ProductDetail;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class SimilarProductsApiIntegrationTest {

    private static final WireMockServer PRODUCT_API = startProductApi();

    @Autowired
    private WebTestClient webTestClient;

    @DynamicPropertySource
    static void productApiProperties(DynamicPropertyRegistry registry) {
        registry.add("similar-products.upstream.base-url", PRODUCT_API::baseUrl);
        registry.add("similar-products.upstream.response-timeout", () -> "5s");
        registry.add("similar-products.lookup-timeout", () -> "1s");
    }

    @AfterAll
    static void stopProductApi() {
        PRODUCT_API.stop();
    }

    @Test
    void returnsTheSimilarProductsOrderedBySimilarity() {
        webTestClient.get().uri("/product/1/similar")
                .exchange()
                .expectStatus().isOk()
                .expectBody().json("""
                        [
                          {"id": "2", "name": "Dress", "price": 19.99, "availability": true},
                          {"id": "3", "name": "Blazer", "price": 29.99, "availability": false},
                          {"id": "4", "name": "Boots", "price": 39.99, "availability": true}
                        ]
                        """, true);
    }

    @Test
    void leavesOutSimilarProductsThatDoNotExist() {
        assertThat(similarProductIds("4")).containsExactly("1", "2");
    }

    @Test
    void leavesOutSimilarProductsThatFail() {
        assertThat(similarProductIds("5")).containsExactly("1", "2");
    }

    @Test
    void doesNotWaitForSlowProductsButIncludesThemOnceTheyAreAvailable() {
        assertThat(similarProductIds("7")).containsExactly("1");

        await().atMost(Duration.ofSeconds(6)).pollInterval(Duration.ofMillis(250))
                .untilAsserted(() -> assertThat(similarProductIds("7")).containsExactly("1", "100"));
        PRODUCT_API.verify(1, getRequestedFor(urlEqualTo("/product/100")));
    }

    @Test
    void returnsNotFoundWhenTheProductDoesNotExist() {
        webTestClient.get().uri("/product/999/similar")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void returnsBadGatewayWhenTheSimilarIdsAreNotAvailable() {
        webTestClient.get().uri("/product/500/similar")
                .exchange()
                .expectStatus().isEqualTo(502);
    }

    @Test
    void returnsGatewayTimeoutWhenTheSimilarIdsDoNotArriveInTime() {
        webTestClient.get().uri("/product/600/similar")
                .exchange()
                .expectStatus().isEqualTo(504);
    }

    private List<String> similarProductIds(String productId) {
        List<ProductDetail> products = webTestClient.get().uri("/product/{productId}/similar", productId)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ProductDetail.class)
                .returnResult()
                .getResponseBody();
        assertThat(products).isNotNull();
        return products.stream().map(ProductDetail::id).toList();
    }

    private static WireMockServer startProductApi() {
        WireMockServer server = new WireMockServer(options().dynamicPort());
        server.start();

        server.stubFor(get("/product/1/similarids").willReturn(okJson("[2,3,4]")));
        server.stubFor(get("/product/4/similarids").willReturn(okJson("[1,2,5]")));
        server.stubFor(get("/product/5/similarids").willReturn(okJson("[1,2,6]")));
        server.stubFor(get("/product/7/similarids").willReturn(okJson("[1,100]")));
        server.stubFor(get("/product/500/similarids").willReturn(serviceUnavailable()));
        server.stubFor(get("/product/999/similarids").willReturn(notFound()));
        // Slower than the test's lookup-timeout (1s) but within its response-timeout (5s),
        // so the request fails with a timeout rather than a connector-level error.
        server.stubFor(get("/product/600/similarids").willReturn(okJson("[1]").withFixedDelay(2000)));


        server.stubFor(get("/product/1").willReturn(okJson("""
                {"id":"1","name":"Shirt","price":9.99,"availability":true}""")));
        server.stubFor(get("/product/2").willReturn(okJson("""
                {"id":"2","name":"Dress","price":19.99,"availability":true}""")));
        server.stubFor(get("/product/3").willReturn(okJson("""
                {"id":"3","name":"Blazer","price":29.99,"availability":false}""")));
        server.stubFor(get("/product/4").willReturn(okJson("""
                {"id":"4","name":"Boots","price":39.99,"availability":true}""")));
        server.stubFor(get("/product/5").willReturn(notFound().withBody("""
                {"message":"Product not found"}""")));
        server.stubFor(get("/product/6").willReturn(serverError()));
        server.stubFor(get("/product/100").willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                        {"id":"100","name":"Trousers","price":49.99,"availability":false}""")
                .withFixedDelay(2500)));
        return server;
    }
}
