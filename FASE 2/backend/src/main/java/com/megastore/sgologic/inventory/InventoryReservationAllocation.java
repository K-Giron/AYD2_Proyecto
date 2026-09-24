package com.megastore.sgologic.inventory;

import java.time.Instant;
import java.util.UUID;

/** Resultado estable del puerto; no expone la entidad JPA de Inventory. */
public record InventoryReservationAllocation(
        UUID reservationId,
        UUID productId,
        int quantity,
        UUID locationId,
        InventoryLocationType locationType,
        String locationCode,
        Instant expiresAt
) {
}
