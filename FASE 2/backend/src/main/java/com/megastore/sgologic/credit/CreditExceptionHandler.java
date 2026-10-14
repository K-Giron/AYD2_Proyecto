package com.megastore.sgologic.credit;

import com.megastore.sgologic.shared.http.ApiError;
import com.megastore.sgologic.shared.http.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice(assignableTypes = {CreditHistoryController.class, CreditOperationsController.class})
class CreditExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> invalid(IllegalArgumentException failure, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_CREDIT_REQUEST", failure.getMessage(), request);
    }

    @ExceptionHandler(CreditOperationNotFoundException.class)
    ResponseEntity<ApiError> notFound(CreditOperationNotFoundException failure, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "CREDIT_OPERATION_NOT_FOUND", failure.getMessage(), request);
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ApiError> conflict(IllegalStateException failure, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "CREDIT_OPERATION_STATE_CONFLICT", failure.getMessage(), request);
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message,
                                            HttpServletRequest request) {
        Object correlationId = request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
        ApiError body = new ApiError(code, message, Instant.now(), request.getRequestURI(),
                correlationId == null ? "unavailable" : correlationId.toString());
        return ResponseEntity.status(status).body(body);
    }
}
