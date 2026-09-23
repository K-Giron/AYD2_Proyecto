package com.megastore.sgologic.inventory;

import java.util.UUID;

public class InventoryItemNotFoundException extends RuntimeException {

    public InventoryItemNotFoundException(UUID productId, UUID locationId) {
        super("No existe stock registrado para el producto " + productId + " en la ubicación " + locationId);
    }
}
