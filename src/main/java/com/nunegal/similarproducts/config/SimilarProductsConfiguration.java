package com.nunegal.similarproducts.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import com.nunegal.similarproducts.application.SimilarProductsService;
import com.nunegal.similarproducts.domain.ProductCatalog;
import com.nunegal.similarproducts.infrastructure.catalog.CachedProductCatalog;
import com.nunegal.similarproducts.infrastructure.catalog.HttpProductCatalog;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;


@Configuration(proxyBeanMethods = false)
public class SimilarProductsConfiguration {

    @Bean(destroyMethod = "dispose")
    ConnectionProvider productCatalogConnectionProvider(SimilarProductsProperties properties) {
        SimilarProductsProperties.Upstream upstream = properties.upstream();
        return ConnectionProvider.builder("product-catalog")
                .maxConnections(upstream.maxConnections())
                .pendingAcquireTimeout(upstream.pendingAcquireTimeout())
                .build();
    }

    @Bean
    WebClient productCatalogWebClient(WebClient.Builder builder,
                                      ConnectionProvider productCatalogConnectionProvider,
                                      SimilarProductsProperties properties) {
        SimilarProductsProperties.Upstream upstream = properties.upstream();
        HttpClient httpClient = HttpClient.create(productCatalogConnectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(upstream.connectTimeout().toMillis()))
                .responseTimeout(upstream.responseTimeout());
        return builder
                .baseUrl(upstream.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Bean
    ProductCatalog productCatalog(WebClient productCatalogWebClient, SimilarProductsProperties properties) {
        return new CachedProductCatalog(new HttpProductCatalog(productCatalogWebClient), properties.cache());
    }

    @Bean
    SimilarProductsService similarProductsService(ProductCatalog productCatalog, SimilarProductsProperties properties) {
        return new SimilarProductsService(productCatalog, properties.lookupTimeout());
    }
}
