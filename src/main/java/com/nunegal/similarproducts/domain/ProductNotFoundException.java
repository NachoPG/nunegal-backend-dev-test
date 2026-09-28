package com.nunegal.similarproducts.domain;

public class ProductNotFoundException extends ProductCatalogException {

    public ProductNotFoundException(String productId) {
        super("Product " + productId + " not found");
    }
}
