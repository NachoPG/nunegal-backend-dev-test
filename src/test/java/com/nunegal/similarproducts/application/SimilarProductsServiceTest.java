package com.nunegal.similarproducts.application;

import static com.nunegal.similarproducts.support.Products.BLAZER;
import static com.nunegal.similarproducts.support.Products.BOOTS;
import static com.nunegal.similarproducts.support.Products.DRESS;
import static com.nunegal.similarproducts.support.Products.SHIRT;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

import com.nunegal.similarproducts.domain.CatalogUnavailableException;
import com.nunegal.similarproducts.domain.ProductDetail;
import com.nunegal.similarproducts.domain.ProductNotFoundException;
import com.nunegal.similarproducts.support.StubProductCatalog;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class SimilarProductsServiceTest {

    private static final Duration LOOKUP_TIMEOUT = Duration.ofMillis(500);

    private final StubProductCatalog catalog = new StubProductCatalog()
            .withProduct(SHIRT)
            .withProduct(DRESS)
            .withProduct(BLAZER)
            .withProduct(BOOTS);

    private final SimilarProductsService service = new SimilarProductsService(catalog, LOOKUP_TIMEOUT);

    @Test
    void returnsTheDetailOfTheSimilarProductsOrderedBySimilarity() {
        catalog.withSimilarIds("1", "2", "3", "4");

        StepVerifier.create(service.findSimilarProducts("1"))
                .expectNext(List.of(DRESS, BLAZER, BOOTS))
                .verifyComplete();
    }

    @Test
    void keepsTheSimilarityOrderWhenDetailsArriveOutOfOrder() {
        catalog.withSimilarIds("1", "2", "3", "4")
                .withProduct("2", Mono.just(DRESS).delayElement(Duration.ofMillis(100)))
                .withProduct("4", Mono.just(BOOTS).delayElement(Duration.ofMillis(50)));

        StepVerifier.create(service.findSimilarProducts("1"))
                .expectNext(List.of(DRESS, BLAZER, BOOTS))
                .verifyComplete();
    }

    @Test
    void looksUpTheDetailsInParallel() {
        Duration delay = Duration.ofMillis(150);
        catalog.withSimilarIds("1", "2", "3", "4")
                .withProduct("2", Mono.just(DRESS).delayElement(delay))
                .withProduct("3", Mono.just(BLAZER).delayElement(delay))
                .withProduct("4", Mono.just(BOOTS).delayElement(delay));

        Duration elapsed = StepVerifier.create(service.findSimilarProducts("1"))
                .expectNext(List.of(DRESS, BLAZER, BOOTS))
                .verifyComplete();

        assertThat(elapsed).isLessThan(delay.multipliedBy(3));
    }

    @Test
    void leavesOutSimilarProductsThatDoNotExist() {
        catalog.withSimilarIds("4", "1", "2", "5");

        StepVerifier.create(service.findSimilarProducts("4"))
                .expectNext(List.of(SHIRT, DRESS))
                .verifyComplete();
    }

    @Test
    void leavesOutSimilarProductsThatFail() {
        catalog.withSimilarIds("5", "1", "2", "6")
                .withProduct("6", Mono.error(new CatalogUnavailableException("500 Internal Server Error")));

        StepVerifier.create(service.findSimilarProducts("5"))
                .expectNext(List.of(SHIRT, DRESS))
                .verifyComplete();
    }

    @Test
    void leavesOutSimilarProductsSlowerThanTheLookupTimeout() {
        catalog.withSimilarIds("2", "3", "1000")
                .withProduct("1000", Mono.never());

        Duration elapsed = StepVerifier.create(service.findSimilarProducts("2"))
                .expectNext(List.of(BLAZER))
                .verifyComplete();

        assertThat(elapsed).isLessThan(LOOKUP_TIMEOUT.multipliedBy(3));
    }

    @Test
    void ignoresRepeatedSimilarIds() {
        catalog.withSimilarIds("1", "2", "3", "2");

        StepVerifier.create(service.findSimilarProducts("1"))
                .expectNext(List.of(DRESS, BLAZER))
                .verifyComplete();
    }

    @Test
    void returnsAnEmptyListWhenThereAreNoSimilarProducts() {
        catalog.withSimilarIds("1");

        StepVerifier.create(service.findSimilarProducts("1"))
                .expectNext(List.<ProductDetail>of())
                .verifyComplete();
    }

    @Test
    void failsWithNotFoundWhenTheProductDoesNotExist() {
        StepVerifier.create(service.findSimilarProducts("unknown"))
                .expectError(ProductNotFoundException.class)
                .verify();
    }

    @Test
    void failsWhenTheSimilarIdsAreNotAvailable() {
        catalog.withSimilarIds("1", Mono.error(new CatalogUnavailableException("503 Service Unavailable")));

        StepVerifier.create(service.findSimilarProducts("1"))
                .expectError(CatalogUnavailableException.class)
                .verify();
    }

    @Test
    void failsWhenTheSimilarIdsTakeLongerThanTheLookupTimeout() {
        catalog.withSimilarIds("1", Mono.never());

        StepVerifier.create(service.findSimilarProducts("1"))
                .expectError(TimeoutException.class)
                .verify(Duration.ofSeconds(2));
    }
}
