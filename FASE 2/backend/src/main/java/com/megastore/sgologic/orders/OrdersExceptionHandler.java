package com.megastore.sgologic.orders;

import com.megastore.sgologic.shared.http.ApiError;
import com.megastore.sgologic.shared.http.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice(basePackageClasses = OrdersExceptionHandler.class)
class OrdersExceptionHandler {

    @ExceptionHandler({CartNotFoundException.class, ActiveCartNotFoundException.class,
            CartItemNotFoundException.class, OrderNotFoundException.class})
    ResponseEntity<ApiError> handleNotFound(RuntimeException exception, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "ORDER_RESOURCE_NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler({CartNotActiveException.class, EmptyCartException.class,
            InvalidOrderTransitionException.class})
    ResponseEntity<ApiError> handleConflict(RuntimeException exception, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "ORDER_STATE_CONFLICT", exception.getMessage(), request);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String message,
                                            HttpServletRequest request) {
        Object correlationId = request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
        ApiError body = new ApiError(code, message, Instant.now(), request.getRequestURI(),
                correlationId == null ? "unavailable" : correlationId.toString());
        return ResponseEntity.status(status).body(body);
    }
}
