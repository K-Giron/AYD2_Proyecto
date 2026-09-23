package com.megastore.sgologic.audit;

import com.megastore.sgologic.shared.http.ApiError;
import com.megastore.sgologic.shared.http.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice(basePackageClasses = AuditExceptionHandler.class)
class AuditExceptionHandler {

    @ExceptionHandler(AuditEvidenceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(AuditEvidenceNotFoundException exception,
                                                    HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "AUDIT_EVIDENCE_NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidAuditQueryException.class)
    public ResponseEntity<ApiError> handleInvalidQuery(InvalidAuditQueryException exception,
                                                        HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_AUDIT_QUERY", exception.getMessage(), request);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String message,
                                            HttpServletRequest request) {
        Object correlationId = request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
        ApiError body = new ApiError(code, message, Instant.now(), request.getRequestURI(),
                correlationId == null ? "unavailable" : correlationId.toString());
        return ResponseEntity.status(status).body(body);
    }
}
