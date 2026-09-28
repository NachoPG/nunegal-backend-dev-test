package com.nunegal.similarproducts.infrastructure.catalog;

import reactor.core.publisher.Mono;


sealed interface Outcome<V> {

    static <V> Outcome<V> success(V value) {
        return new Success<>(value);
    }

    static <V> Outcome<V> failure(Throwable error) {
        return new Failure<>(error);
    }

    Mono<V> toMono();

    record Success<V>(V value) implements Outcome<V> {

        @Override
        public Mono<V> toMono() {
            return Mono.just(value);
        }
    }

    record Failure<V>(Throwable error) implements Outcome<V> {

        @Override
        public Mono<V> toMono() {
            return Mono.error(error);
        }
    }
}
