package com.nunegal.similarproducts.infrastructure.catalog;

import java.util.List;
import java.util.Objects;

import com.github.benmanes.caffeine.cache.Ticker;
import com.nunegal.similarproducts.config.SimilarProductsProperties;
import com.nunegal.similarproducts.domain.ProductCatalog;
import com.nunegal.similarproducts.domain.ProductDetail;

import reactor.core.publisher.Mono;


public class CachedProductCatalog implements ProductCatalog {

    private final ProductCatalog delegate;
    private final OutcomeCache<List<String>> similarIds;
    private final OutcomeCache<ProductDetail> products;

    public CachedProductCatalog(ProductCatalog delegate, SimilarProductsProperties.Cache settings) {
        this(delegate, settings, Ticker.systemTicker());
    }

    CachedProductCatalog(ProductCatalog delegate, SimilarProductsProperties.Cache settings, Ticker ticker) {
        this.delegate = Objects.requireNonNull(delegate);
        this.similarIds = new OutcomeCache<>(settings, ticker);
        this.products = new OutcomeCache<>(settings, ticker);
    }

    @Override
    public Mono<List<String>> findSimilarIds(String productId) {
        return similarIds.get(productId, delegate::findSimilarIds);
    }

    @Override
    public Mono<ProductDetail> findProduct(String productId) {
        return products.get(productId, delegate::findProduct);
    }
}
