package com.megastore.sgologic.inventory;

import java.time.Instant;
import java.util.UUID;

/**
 * Vista pública de disponibilidad, consumida por otros módulos (Órdenes,
 * Despacho) a través del puerto de consulta, nunca leyendo la tabla
 * {@code inventory_items} directamente (ver ORD-03,
 * {@code saga-required-ports.md}).
 */
public record InventoryStockResponse(
        UUID productId,
        UUID locationId,
        String locationCode,
        String locationName,
        int quantityOnHand,
        int quantityReserved,
        int quantityAvailable,
        Instant updatedAt
) {
    static InventoryStockResponse from(InventoryItem item, InventoryLocation location) {
        return new InventoryStockResponse(
                item.getProductId(),
                location.getId(),
                location.getCode(),
                location.getName(),
                item.getQuantityOnHand(),
                item.getQuantityReserved(),
                item.getQuantityAvailable(),
                item.getUpdatedAt());
    }
}
