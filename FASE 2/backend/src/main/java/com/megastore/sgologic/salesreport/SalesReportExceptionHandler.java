package com.megastore.sgologic.salesreport;

import com.megastore.sgologic.shared.http.ApiError;
import com.megastore.sgologic.shared.http.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice(basePackageClasses = SalesReportExceptionHandler.class)
class SalesReportExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> invalidRange(IllegalArgumentException exception, HttpServletRequest request) {
        Object correlationId = request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError("INVALID_REPORT_RANGE",
                exception.getMessage(), Instant.now(), request.getRequestURI(),
                correlationId == null ? "unavailable" : correlationId.toString()));
    }
}
