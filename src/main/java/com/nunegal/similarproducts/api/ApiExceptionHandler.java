package com.nunegal.similarproducts.api;

import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.nunegal.similarproducts.domain.CatalogUnavailableException;
import com.nunegal.similarproducts.domain.ProductNotFoundException;


@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);


    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception error) {
        log.error("Unexpected error handling similar products request", error);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
    }

    @ExceptionHandler(ProductNotFoundException.class)
    ResponseEntity<ProblemDetail> handleProductNotFound(ProductNotFoundException error) {
        return problem(HttpStatus.NOT_FOUND, error.getMessage());
    }

    @ExceptionHandler(CatalogUnavailableException.class)
    ResponseEntity<ProblemDetail> handleCatalogUnavailable(CatalogUnavailableException error) {
        log.debug("Similar products not available: {}", error.getMessage());
        return problem(HttpStatus.BAD_GATEWAY, "Similar products are not available right now");
    }

    @ExceptionHandler(TimeoutException.class)
    ResponseEntity<ProblemDetail> handleTimeout(TimeoutException error) {
        log.debug("Similar products lookup timed out: {}", error.getMessage());
        return problem(HttpStatus.GATEWAY_TIMEOUT, "Similar products could not be retrieved in time");
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
    }
}
