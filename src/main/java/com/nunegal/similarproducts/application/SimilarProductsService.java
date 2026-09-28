package com.nunegal.similarproducts.application;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nunegal.similarproducts.domain.ProductCatalog;
import com.nunegal.similarproducts.domain.ProductDetail;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public class SimilarProductsService {

    private static final Logger log = LoggerFactory.getLogger(SimilarProductsService.class);

    private final ProductCatalog catalog;
    private final Duration lookupTimeout;

    public SimilarProductsService(ProductCatalog catalog, Duration lookupTimeout) {
        this.catalog = Objects.requireNonNull(catalog);
        this.lookupTimeout = Objects.requireNonNull(lookupTimeout);
    }

    public Mono<List<ProductDetail>> findSimilarProducts(String productId) {
        return catalog.findSimilarIds(productId)
                .timeout(lookupTimeout)
                .flatMapMany(Flux::fromIterable)
                .distinct()
                .flatMapSequential(this::findProductOrSkip)
                .collectList();
    }

    private Mono<ProductDetail> findProductOrSkip(String productId) {
        return catalog.findProduct(productId)
                .timeout(lookupTimeout)
                .onErrorResume(error -> {
                    log.debug("Similar product {} left out of the response: {}", productId, error.toString());
                    return Mono.empty();
                });
    }
}
