package com.nunegal.similarproducts.infrastructure.catalog;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.Ticker;
import com.nunegal.similarproducts.config.SimilarProductsProperties;
import com.nunegal.similarproducts.domain.ProductNotFoundException;

import reactor.core.publisher.Mono;


final class OutcomeCache<V> {

    private final AsyncCache<String, Outcome<V>> cache;

    OutcomeCache(SimilarProductsProperties.Cache settings, Ticker ticker) {
        this.cache = Caffeine.newBuilder()
                .maximumSize(settings.maxSize())
                .expireAfter(new OutcomeExpiry<V>(settings))
                .ticker(ticker)
                .buildAsync();
    }


    Mono<V> get(String key, Function<String, Mono<V>> loader) {
        return Mono.defer(() -> {
                    CompletableFuture<Outcome<V>> outcome = cache.get(key, (k, executor) -> load(k, loader));
                    // suppressCancel: a caller giving up must not cancel the call shared with other callers
                    return Mono.fromFuture(outcome, true);
                })
                .flatMap(Outcome::toMono);
    }

    private static <V> CompletableFuture<Outcome<V>> load(String key, Function<String, Mono<V>> loader) {
        return loader.apply(key)
                .map(Outcome::success)
                .onErrorResume(error -> Mono.just(Outcome.<V>failure(error)))
                .toFuture();
    }

    private record OutcomeExpiry<V>(SimilarProductsProperties.Cache settings) implements Expiry<String, Outcome<V>> {

        @Override
        public long expireAfterCreate(String key, Outcome<V> outcome, long currentTime) {
            return timeToLive(outcome).toNanos();
        }

        @Override
        public long expireAfterUpdate(String key, Outcome<V> outcome, long currentTime, long currentDuration) {
            return timeToLive(outcome).toNanos();
        }

        @Override
        public long expireAfterRead(String key, Outcome<V> outcome, long currentTime, long currentDuration) {
            return currentDuration;
        }

        private Duration timeToLive(Outcome<V> outcome) {
            return switch (outcome) {
                case Outcome.Success<V> success -> settings.successTtl();
                case Outcome.Failure<V> failure when failure.error() instanceof ProductNotFoundException ->
                        settings.notFoundTtl();
                case Outcome.Failure<V> failure -> settings.errorTtl();
            };
        }
    }
}
