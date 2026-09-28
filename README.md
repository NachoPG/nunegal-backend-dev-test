# nunegal-backend-dev-test

Solution to the *Backend dev technical test*: a Spring Boot application that exposes, on port **5000**, the operation agreed with the front-end team to get the product detail of the products similar to a given one.

```
GET /product/{productId}/similar  ->  200 [ProductDetail, ...] ordered by similarity
                                      404 if the product does not exist
```

The contracts are in [`docs/similarProducts.yaml`](docs/similarProducts.yaml) (the exposed API) and [`docs/existingApis.yaml`](docs/existingApis.yaml) (the existing APIs it consumes).

## Requirements

- JDK 21 and Maven 3.9+ to build and run locally.
- Docker for the mocks, the load test and (optionally) to run the application without a JDK or Maven.

## How to run it

```bash
# 1. Test mocks and infrastructure
docker compose up -d simulado influxdb grafana

# 2a. Application locally
mvn spring-boot:run

# 2b. ...or as a container
docker compose up -d --build similar-products

# 3. Manual test
curl http://localhost:5000/product/1/similar

# 4. Load test (results at http://localhost:3000/d/Le2Ku9NMk/k6-performance-test)
docker compose run --rm k6 run scripts/test.js
```

The `shared/` folder is a copy of the technical test's own test infrastructure (simulado mocks, the k6 script and the Grafana dashboard), so the application can be evaluated from this same repository. On Docker Desktop you may need to enable *file sharing* for that folder.

Automated tests (unit and integration, no Docker needed):

```bash
mvn verify
```

## Configuration

All properties are in [`application.yml`](src/main/resources/application.yml) under the `similar-products` prefix and are validated on startup.

| Property | Default value | Description |
|---|---|---|
| `lookup-timeout` | `1s` | Maximum time a request waits for each piece of data. |
| `upstream.base-url` | `http://localhost:3001` | URL of the existing APIs (environment variable `PRODUCT_API_URL`). |
| `upstream.connect-timeout` | `500ms` | Connection timeout. |
| `upstream.response-timeout` | `10s` | Hard limit for a single upstream call. |
| `upstream.max-connections` | `500` | Size of the connection pool. |
| `upstream.pending-acquire-timeout` | `5s` | Maximum wait for a free connection from the pool. |
| `cache.max-size` | `10000` | Maximum entries per cache. |
| `cache.success-ttl` | `60s` | Lifetime of a successful result. |
| `cache.not-found-ttl` | `60s` | Lifetime of a "product not found" answer. |
| `cache.error-ttl` | `5s` | Lifetime of an error before retrying. |

The application also exposes `/actuator/health`, `/actuator/info` and `/actuator/metrics`.
