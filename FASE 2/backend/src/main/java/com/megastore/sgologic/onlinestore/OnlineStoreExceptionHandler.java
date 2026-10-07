package com.megastore.sgologic.onlinestore;

import com.megastore.sgologic.shared.http.ApiError;
import com.megastore.sgologic.shared.http.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice(basePackageClasses = OnlineStoreExceptionHandler.class)
class OnlineStoreExceptionHandler {

    @ExceptionHandler(OnlineStoreException.class)
    public ResponseEntity<ApiError> handle(OnlineStoreException exception, HttpServletRequest request) {
        Object correlationId = request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
        ApiError body = new ApiError(exception.getCode(), exception.getMessage(), Instant.now(),
                request.getRequestURI(), correlationId == null ? "unavailable" : correlationId.toString());
        return ResponseEntity.status(exception.getStatus()).body(body);
    }
}
