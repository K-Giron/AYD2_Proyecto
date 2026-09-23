package com.megastore.sgologic.inventory;

import java.time.Instant;
import java.util.UUID;

/**
 * Resultado mínimo propuesto por {@code saga-required-ports.md} para el
 * puerto de reserva: {@code reservationId}, origen (ubicación), expiración
 * y estado.
 */
public record InventoryReservationResponse(
        UUID reservationId,
        UUID orderId,
        UUID productId,
        UUID locationId,
        int quantity,
        InventoryReservationStatus status,
        UUID correlationId,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt
) {
    static InventoryReservationResponse from(InventoryReservation reservation) {
        return new InventoryReservationResponse(
                reservation.getId(),
                reservation.getOrderId(),
                reservation.getProductId(),
                reservation.getLocationId(),
                reservation.getQuantity(),
                reservation.getStatus(),
                reservation.getCorrelationId(),
                reservation.getExpiresAt(),
                reservation.getCreatedAt(),
                reservation.getUpdatedAt());
    }
}
