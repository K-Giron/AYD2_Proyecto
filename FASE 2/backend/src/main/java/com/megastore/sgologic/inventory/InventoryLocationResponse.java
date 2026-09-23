package com.megastore.sgologic.inventory;

import java.util.UUID;

public record InventoryLocationResponse(
        UUID locationId,
        String code,
        String name,
        InventoryLocationType type
) {
    static InventoryLocationResponse from(InventoryLocation location) {
        return new InventoryLocationResponse(
                location.getId(),
                location.getCode(),
                location.getName(),
                location.getType());
    }
}
