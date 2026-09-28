package com.nunegal.similarproducts.domain;

import java.util.List;

import reactor.core.publisher.Mono;


public interface ProductCatalog {


    Mono<List<String>> findSimilarIds(String productId);

    Mono<ProductDetail> findProduct(String productId);
}
