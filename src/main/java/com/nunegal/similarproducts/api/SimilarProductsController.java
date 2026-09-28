package com.nunegal.similarproducts.api;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.nunegal.similarproducts.application.SimilarProductsService;
import com.nunegal.similarproducts.domain.ProductDetail;

import reactor.core.publisher.Mono;

@RestController
public class SimilarProductsController {

    private final SimilarProductsService similarProductsService;

    public SimilarProductsController(SimilarProductsService similarProductsService) {
        this.similarProductsService = similarProductsService;
    }

    @GetMapping(path = "/product/{productId}/similar", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<List<ProductDetail>> getSimilarProducts(@PathVariable("productId") String productId) {
        return similarProductsService.findSimilarProducts(productId);
    }
}
