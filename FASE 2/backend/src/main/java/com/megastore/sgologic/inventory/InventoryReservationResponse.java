package com.megastore.sgologic.inventory;

import java.time.Instant;
import java.util.UUID;

/**
 * Resultado mínimo propuesto por {@code saga-required-ports.md} para el
 * puerto de reserva: {@code reservationId}, origen (ubicación), expiración
 * y estado.
 *
 * <p>{@code locationType}/{@code locationCode} van más allá de ese mínimo:
 * evitan que un consumidor (p. ej. Logistics/LOG-01, que necesita mapear el
 * origen a su propio {@code DispatchOrigin}) tenga que hacer una segunda
 * llamada a {@code GET /api/v1/inventory/locations} solo para resolver el
 * tipo de la ubicación elegida por INV-04.</p>
 */
public record InventoryReservationResponse(
        UUID reservationId,
        UUID orderId,
        UUID productId,
        UUID locationId,
        InventoryLocationType locationType,
        String locationCode,
        int quantity,
        InventoryReservationStatus status,
        UUID correlationId,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt
) {
    static InventoryReservationResponse from(InventoryReservation reservation, InventoryLocation location) {
        return new InventoryReservationResponse(
                reservation.getId(),
                reservation.getOrderId(),
                reservation.getProductId(),
                reservation.getLocationId(),
                location.getType(),
                location.getCode(),
                reservation.getQuantity(),
                reservation.getStatus(),
                reservation.getCorrelationId(),
                reservation.getExpiresAt(),
                reservation.getCreatedAt(),
                reservation.getUpdatedAt());
    }
}
