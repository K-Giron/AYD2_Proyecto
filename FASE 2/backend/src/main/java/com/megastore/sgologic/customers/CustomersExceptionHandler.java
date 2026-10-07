package com.megastore.sgologic.customers;

import com.megastore.sgologic.shared.http.ApiError;
import com.megastore.sgologic.shared.http.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice(basePackageClasses = CustomersExceptionHandler.class)
class CustomersExceptionHandler {

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(CustomerNotFoundException exception, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "CUSTOMER_NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler(DuplicateCustomerUsernameException.class)
    public ResponseEntity<ApiError> handleDuplicate(DuplicateCustomerUsernameException exception,
                                                    HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "DUPLICATE_CUSTOMER_USERNAME", exception.getMessage(), request);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String message,
                                            HttpServletRequest request) {
        Object correlationId = request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
        ApiError body = new ApiError(code, message, Instant.now(), request.getRequestURI(),
                correlationId == null ? "unavailable" : correlationId.toString());
        return ResponseEntity.status(status).body(body);
    }
}
