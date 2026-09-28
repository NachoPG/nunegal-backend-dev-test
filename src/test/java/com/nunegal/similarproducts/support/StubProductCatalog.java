package com.nunegal.similarproducts.support;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.nunegal.similarproducts.domain.ProductCatalog;
import com.nunegal.similarproducts.domain.ProductDetail;
import com.nunegal.similarproducts.domain.ProductNotFoundException;

import reactor.core.publisher.Mono;


public final class StubProductCatalog implements ProductCatalog {

    private final Map<String, Mono<List<String>>> similarIds = new ConcurrentHashMap<>();
    private final Map<String, Mono<ProductDetail>> products = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> productLookups = new ConcurrentHashMap<>();

    public StubProductCatalog withSimilarIds(String productId, String... ids) {
        return withSimilarIds(productId, Mono.just(List.of(ids)));
    }

    public StubProductCatalog withSimilarIds(String productId, Mono<List<String>> response) {
        similarIds.put(productId, response);
        return this;
    }

    public StubProductCatalog withProduct(ProductDetail product) {
        return withProduct(product.id(), Mono.just(product));
    }

    public StubProductCatalog withProduct(String productId, Mono<ProductDetail> response) {
        products.put(productId, response);
        return this;
    }

    public int productLookups(String productId) {
        AtomicInteger lookups = productLookups.get(productId);
        return lookups == null ? 0 : lookups.get();
    }

    @Override
    public Mono<List<String>> findSimilarIds(String productId) {
        return similarIds.getOrDefault(productId, Mono.error(new ProductNotFoundException(productId)));
    }

    @Override
    public Mono<ProductDetail> findProduct(String productId) {
        productLookups.computeIfAbsent(productId, id -> new AtomicInteger()).incrementAndGet();
        return products.getOrDefault(productId, Mono.error(new ProductNotFoundException(productId)));
    }
}
