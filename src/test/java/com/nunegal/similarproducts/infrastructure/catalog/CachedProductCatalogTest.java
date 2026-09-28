package com.nunegal.similarproducts.infrastructure.catalog;

import static com.nunegal.similarproducts.support.Products.SHIRT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import com.github.benmanes.caffeine.cache.Ticker;
import com.nunegal.similarproducts.config.SimilarProductsProperties;
import com.nunegal.similarproducts.domain.CatalogUnavailableException;
import com.nunegal.similarproducts.domain.ProductDetail;
import com.nunegal.similarproducts.domain.ProductNotFoundException;
import com.nunegal.similarproducts.support.StubProductCatalog;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

class CachedProductCatalogTest {

    private static final Duration SUCCESS_TTL = Duration.ofSeconds(60);
    private static final Duration NOT_FOUND_TTL = Duration.ofSeconds(30);
    private static final Duration ERROR_TTL = Duration.ofSeconds(5);

    private final FakeTicker ticker = new FakeTicker();
    private final StubProductCatalog upstream = new StubProductCatalog();
    private final CachedProductCatalog catalog = new CachedProductCatalog(upstream,
            new SimilarProductsProperties.Cache(100, SUCCESS_TTL, NOT_FOUND_TTL, ERROR_TTL), ticker);

    @Test
    void sharesASingleUpstreamCallBetweenConcurrentLookups() {
        Sinks.One<ProductDetail> upstreamResponse = Sinks.one();
        upstream.withProduct("1", upstreamResponse.asMono());

        StepVerifier first = StepVerifier.create(catalog.findProduct("1")).expectNext(SHIRT).expectComplete().verifyLater();
        StepVerifier second = StepVerifier.create(catalog.findProduct("1")).expectNext(SHIRT).expectComplete().verifyLater();
        upstreamResponse.tryEmitValue(SHIRT);

        first.verify(Duration.ofSeconds(1));
        second.verify(Duration.ofSeconds(1));
        assertThat(upstream.productLookups("1")).isEqualTo(1);
    }

    @Test
    void cachesProductsUntilTheSuccessTtlExpires() {
        upstream.withProduct(SHIRT);

        lookUpProduct("1").expectNext(SHIRT).verifyComplete();
        ticker.advance(SUCCESS_TTL.minusSeconds(1));
        lookUpProduct("1").expectNext(SHIRT).verifyComplete();
        assertThat(upstream.productLookups("1")).isEqualTo(1);

        ticker.advance(Duration.ofSeconds(2));
        lookUpProduct("1").expectNext(SHIRT).verifyComplete();
        assertThat(upstream.productLookups("1")).isEqualTo(2);
    }

    @Test
    void cachesNotFoundAnswersUntilTheNotFoundTtlExpires() {
        lookUpProduct("5").expectError(ProductNotFoundException.class).verify();
        ticker.advance(NOT_FOUND_TTL.minusSeconds(1));
        lookUpProduct("5").expectError(ProductNotFoundException.class).verify();
        assertThat(upstream.productLookups("5")).isEqualTo(1);

        ticker.advance(Duration.ofSeconds(2));
        lookUpProduct("5").expectError(ProductNotFoundException.class).verify();
        assertThat(upstream.productLookups("5")).isEqualTo(2);
    }

    @Test
    void cachesFailuresOnlyUntilTheErrorTtlExpires() {
        upstream.withProduct("6", Mono.error(new CatalogUnavailableException("500 Internal Server Error")));

        lookUpProduct("6").expectError(CatalogUnavailableException.class).verify();
        lookUpProduct("6").expectError(CatalogUnavailableException.class).verify();
        assertThat(upstream.productLookups("6")).isEqualTo(1);

        ticker.advance(ERROR_TTL.plusSeconds(1));
        lookUpProduct("6").expectError(CatalogUnavailableException.class).verify();
        assertThat(upstream.productLookups("6")).isEqualTo(2);
    }

    @Test
    void completesAndCachesLookupsAbandonedByTheCaller() {
        upstream.withProduct("1", Mono.just(SHIRT).delayElement(Duration.ofMillis(300)));

        StepVerifier.create(catalog.findProduct("1").timeout(Duration.ofMillis(50)))
                .expectError(TimeoutException.class)
                .verify();

        await().atMost(Duration.ofSeconds(2)).untilAsserted(() ->
                StepVerifier.create(catalog.findProduct("1").timeout(Duration.ofMillis(50)))
                        .expectNext(SHIRT)
                        .verifyComplete());
        assertThat(upstream.productLookups("1")).isEqualTo(1);
    }

    private StepVerifier.FirstStep<ProductDetail> lookUpProduct(String productId) {
        return StepVerifier.create(catalog.findProduct(productId));
    }

    private static final class FakeTicker implements Ticker {

        private final AtomicLong nanos = new AtomicLong();

        @Override
        public long read() {
            return nanos.get();
        }

        void advance(Duration duration) {
            nanos.addAndGet(duration.toNanos());
        }
    }
}
