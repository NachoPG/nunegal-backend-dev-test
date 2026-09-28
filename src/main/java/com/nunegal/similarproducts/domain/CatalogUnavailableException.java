package com.nunegal.similarproducts.domain;


public class CatalogUnavailableException extends ProductCatalogException {

    public CatalogUnavailableException(String message) {
        super(message);
    }

    public CatalogUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
