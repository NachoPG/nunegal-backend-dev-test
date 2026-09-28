package com.nunegal.similarproducts.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;


@Validated
@ConfigurationProperties(prefix = "similar-products")
public record SimilarProductsProperties(
        @NotNull Duration lookupTimeout,
        @Valid @NotNull Upstream upstream,
        @Valid @NotNull Cache cache) {

    /**
     * @param baseUrl               base URL of the existing product APIs
     * @param connectTimeout        maximum time to establish a connection
     * @param responseTimeout       hard limit for a single call, after which it is aborted
     * @param maxConnections        size of the connection pool
     * @param pendingAcquireTimeout maximum time to wait for a free connection of the pool
     */
    public record Upstream(
            @NotBlank String baseUrl,
            @NotNull Duration connectTimeout,
            @NotNull Duration responseTimeout,
            @Positive int maxConnections,
            @NotNull Duration pendingAcquireTimeout) {
    }

    /**
     * @param maxSize     maximum number of cached lookups (per lookup type)
     * @param successTtl  how long a successful lookup is cached
     * @param notFoundTtl how long a "product not found" answer is cached
     * @param errorTtl    how long a failed lookup is cached before the upstream is called again
     */
    public record Cache(
            @Positive long maxSize,
            @NotNull Duration successTtl,
            @NotNull Duration notFoundTtl,
            @NotNull Duration errorTtl) {
    }
}
