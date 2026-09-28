package com.nunegal.similarproducts.support;

import java.math.BigDecimal;

import com.nunegal.similarproducts.domain.ProductDetail;


public final class Products {

    public static final ProductDetail SHIRT = new ProductDetail("1", "Shirt", new BigDecimal("9.99"), true);
    public static final ProductDetail DRESS = new ProductDetail("2", "Dress", new BigDecimal("19.99"), true);
    public static final ProductDetail BLAZER = new ProductDetail("3", "Blazer", new BigDecimal("29.99"), false);
    public static final ProductDetail BOOTS = new ProductDetail("4", "Boots", new BigDecimal("39.99"), true);

    private Products() {
    }
}
