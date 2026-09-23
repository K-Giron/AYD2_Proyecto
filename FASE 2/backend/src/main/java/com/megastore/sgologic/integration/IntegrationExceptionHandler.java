package com.megastore.sgologic.integration;

import com.megastore.sgologic.shared.http.ApiError;
import com.megastore.sgologic.shared.http.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice(basePackageClasses = IntegrationExceptionHandler.class)
class IntegrationExceptionHandler {

    @ExceptionHandler(IntegrationResourceNotFoundException.class)
    ResponseEntity<ApiError> notFound(IntegrationResourceNotFoundException failure, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "INTEGRATION_RESOURCE_NOT_FOUND", failure.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> invalid(IllegalArgumentException failure, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_INTEGRATION_REQUEST", failure.getMessage(), request);
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message,
                                            HttpServletRequest request) {
        Object correlationId = request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
        ApiError body = new ApiError(code, message, Instant.now(), request.getRequestURI(),
                correlationId == null ? "unavailable" : correlationId.toString());
        return ResponseEntity.status(status).body(body);
    }
}
