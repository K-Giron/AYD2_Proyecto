package com.megastore.sgologic.inventory;

import com.megastore.sgologic.shared.http.ApiError;
import com.megastore.sgologic.shared.http.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

/**
 * Traduce las excepciones propias de Inventory al formato común de error
 * ({@code shared.http.ApiError}) sin mover reglas de negocio a
 * {@code shared}. Alcance limitado a este módulo mediante
 * {@code basePackageClasses}.
 */
@RestControllerAdvice(basePackageClasses = InventoryExceptionHandler.class)
class InventoryExceptionHandler {

    @ExceptionHandler(InventoryItemNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(InventoryItemNotFoundException exception,
                                                     HttpServletRequest request) {
        Object correlationId = request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
        ApiError body = new ApiError(
                "INVENTORY_ITEM_NOT_FOUND",
                exception.getMessage(),
                Instant.now(),
                request.getRequestURI(),
                correlationId == null ? "unavailable" : correlationId.toString());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }
}
