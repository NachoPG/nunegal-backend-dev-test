package com.nunegal.similarproducts.domain;

public abstract class ProductCatalogException extends RuntimeException {

    protected ProductCatalogException(String message) {
        super(message);
    }

    protected ProductCatalogException(String message, Throwable cause) {
        super(message, cause);
    }
}
