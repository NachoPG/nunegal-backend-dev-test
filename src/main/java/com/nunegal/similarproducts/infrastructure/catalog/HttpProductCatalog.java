package com.nunegal.similarproducts.infrastructure.catalog;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.WebClient;

import com.nunegal.similarproducts.domain.CatalogUnavailableException;
import com.nunegal.similarproducts.domain.ProductCatalog;
import com.nunegal.similarproducts.domain.ProductCatalogException;
import com.nunegal.similarproducts.domain.ProductDetail;
import com.nunegal.similarproducts.domain.ProductNotFoundException;

import reactor.core.publisher.Mono;


public class HttpProductCatalog implements ProductCatalog {

    private static final Logger log = LoggerFactory.getLogger(HttpProductCatalog.class);

    private static final ParameterizedTypeReference<List<String>> ID_LIST = new ParameterizedTypeReference<>() {
    };

    private final WebClient webClient;

    public HttpProductCatalog(WebClient webClient) {
        this.webClient = Objects.requireNonNull(webClient);
    }

    @Override
    public Mono<List<String>> findSimilarIds(String productId) {
        return webClient.get()
                .uri("/product/{productId}/similarids", productId)
                .retrieve()
                .onStatus(HttpProductCatalog::isNotFound, response -> Mono.error(new ProductNotFoundException(productId)))
                .bodyToMono(ID_LIST)
                .defaultIfEmpty(List.of())
                .transform(translateErrors("similar ids of product " + productId));
    }

    @Override
    public Mono<ProductDetail> findProduct(String productId) {
        return webClient.get()
                .uri("/product/{productId}", productId)
                .retrieve()
                .onStatus(HttpProductCatalog::isNotFound, response -> Mono.error(new ProductNotFoundException(productId)))
                .bodyToMono(ProductDetail.class)
                .switchIfEmpty(Mono.error(() -> new CatalogUnavailableException("Empty response for product " + productId)))
                .transform(translateErrors("product " + productId));
    }

    private static boolean isNotFound(HttpStatusCode status) {
        return status.value() == HttpStatus.NOT_FOUND.value();
    }

    private static <T> Function<Mono<T>, Mono<T>> translateErrors(String lookup) {
        return mono -> mono
                .onErrorMap(error -> !(error instanceof ProductCatalogException),
                        error -> new CatalogUnavailableException("Could not retrieve " + lookup + ": " + describe(error), error))
                .doOnError(CatalogUnavailableException.class, error -> log.warn(error.getMessage()));
    }

    private static String describe(Throwable error) {
        return error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName();
    }
}
