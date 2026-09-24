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
    public ResponseEntity<ApiError> handleItemNotFound(InventoryItemNotFoundException exception,
                                                         HttpServletRequest request) {
        return errorResponse(HttpStatus.NOT_FOUND, "INVENTORY_ITEM_NOT_FOUND", exception, request);
    }

    @ExceptionHandler(InventoryReservationNotFoundException.class)
    public ResponseEntity<ApiError> handleReservationNotFound(InventoryReservationNotFoundException exception,
                                                                HttpServletRequest request) {
        return errorResponse(HttpStatus.NOT_FOUND, "INVENTORY_RESERVATION_NOT_FOUND", exception, request);
    }

    @ExceptionHandler(InventoryLocationNotFoundException.class)
    public ResponseEntity<ApiError> handleLocationNotFound(InventoryLocationNotFoundException exception,
                                                             HttpServletRequest request) {
        return errorResponse(HttpStatus.NOT_FOUND, "INVENTORY_LOCATION_NOT_FOUND", exception, request);
    }

    /** CDU-02 E1: incluye el caso de dos solicitudes compitiendo por la última unidad. */
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiError> handleInsufficientStock(InsufficientStockException exception,
                                                              HttpServletRequest request) {
        return errorResponse(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK", exception, request);
    }

    /** INV-04: ninguna ubicación candidata tenía disponibilidad suficiente. */
    @ExceptionHandler(NoAvailableOriginException.class)
    public ResponseEntity<ApiError> handleNoAvailableOrigin(NoAvailableOriginException exception,
                                                              HttpServletRequest request) {
        return errorResponse(HttpStatus.CONFLICT, "NO_AVAILABLE_ORIGIN", exception, request);
    }

    /** Salida de inventario que excede la existencia disponible (no reservada). */
    @ExceptionHandler(InsufficientOnHandStockException.class)
    public ResponseEntity<ApiError> handleInsufficientOnHandStock(InsufficientOnHandStockException exception,
                                                                     HttpServletRequest request) {
        return errorResponse(HttpStatus.CONFLICT, "INSUFFICIENT_ON_HAND_STOCK", exception, request);
    }

    private ResponseEntity<ApiError> errorResponse(HttpStatus status, String code, RuntimeException exception,
                                                     HttpServletRequest request) {
        Object correlationId = request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
        ApiError body = new ApiError(
                code,
                exception.getMessage(),
                Instant.now(),
                request.getRequestURI(),
                correlationId == null ? "unavailable" : correlationId.toString());
        return ResponseEntity.status(status).body(body);
    }
}
