package com.megastore.sgologic.payments;

import com.megastore.sgologic.shared.http.ApiError;
import com.megastore.sgologic.shared.http.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice(basePackageClasses = PaymentsExceptionHandler.class)
class PaymentsExceptionHandler {
    @ExceptionHandler(PaymentAttemptNotFoundException.class)
    ResponseEntity<ApiError> notFound(RuntimeException error, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "PAYMENT_ATTEMPT_NOT_FOUND", error, request);
    }

    @ExceptionHandler({UnsupportedPaymentMethodException.class, IllegalArgumentException.class})
    ResponseEntity<ApiError> invalid(RuntimeException error, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "PAYMENT_REQUEST_INVALID", error, request);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String code, RuntimeException error,
                                            HttpServletRequest request) {
        Object correlationId = request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
        return ResponseEntity.status(status).body(new ApiError(code, error.getMessage(), Instant.now(),
                request.getRequestURI(), correlationId == null ? "unavailable" : correlationId.toString()));
    }
}
