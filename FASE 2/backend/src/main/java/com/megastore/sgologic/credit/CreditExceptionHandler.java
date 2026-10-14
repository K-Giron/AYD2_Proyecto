package com.megastore.sgologic.credit;

import com.megastore.sgologic.shared.http.ApiError;
import com.megastore.sgologic.shared.http.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice(assignableTypes = CreditHistoryController.class)
class CreditExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> invalid(IllegalArgumentException failure, HttpServletRequest request) {
        Object correlationId = request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
        ApiError body = new ApiError("INVALID_CREDIT_HISTORY", failure.getMessage(), Instant.now(),
                request.getRequestURI(), correlationId == null ? "unavailable" : correlationId.toString());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
