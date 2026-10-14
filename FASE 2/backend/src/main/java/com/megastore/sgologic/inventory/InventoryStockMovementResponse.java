package com.megastore.sgologic.inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryStockMovementResponse(
        UUID movementId,
        UUID productId,
        UUID locationId,
        InventoryLocationType locationType,
        String locationCode,
        StockMovementType type,
        int quantity,
        String reason,
        int quantityOnHandAfter,
        UUID correlationId,
        Instant occurredAt
) {
    static InventoryStockMovementResponse from(InventoryStockMovement movement, InventoryLocation location,
                                                int quantityOnHandAfter) {
        return new InventoryStockMovementResponse(
                movement.getId(),
                movement.getProductId(),
                movement.getLocationId(),
                location.getType(),
                location.getCode(),
                movement.getType(),
                movement.getQuantity(),
                movement.getReason(),
                quantityOnHandAfter,
                movement.getCorrelationId(),
                movement.getOccurredAt());
    }
}
