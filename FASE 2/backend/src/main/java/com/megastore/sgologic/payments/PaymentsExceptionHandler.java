package com.megastore.sgologic.payments;

import com.megastore.sgologic.payments.api.InvalidPaymentRequestException;
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
    public ResponseEntity<ApiError> handleNotFound(PaymentAttemptNotFoundException exception,
                                                    HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "PAYMENT_ATTEMPT_NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidPaymentRequestException.class)
    public ResponseEntity<ApiError> handleInvalidRequest(InvalidPaymentRequestException exception,
                                                          HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_PAYMENT_REQUEST", exception.getMessage(), request);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String message,
                                            HttpServletRequest request) {
        Object correlationId = request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
        ApiError body = new ApiError(code, message, Instant.now(), request.getRequestURI(),
                correlationId == null ? "unavailable" : correlationId.toString());
        return ResponseEntity.status(status).body(body);
    }
}
