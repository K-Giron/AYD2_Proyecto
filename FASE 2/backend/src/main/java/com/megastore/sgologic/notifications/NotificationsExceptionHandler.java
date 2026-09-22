package com.megastore.sgologic.notifications;

import com.megastore.sgologic.shared.http.ApiError;
import com.megastore.sgologic.shared.http.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice(basePackageClasses = NotificationsExceptionHandler.class)
class NotificationsExceptionHandler {

    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotificationNotFoundException exception,
                                                     HttpServletRequest request) {
        Object correlationId = request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
        ApiError body = new ApiError(
                "NOTIFICATION_NOT_FOUND",
                exception.getMessage(),
                Instant.now(),
                request.getRequestURI(),
                correlationId == null ? "unavailable" : correlationId.toString());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }
}
