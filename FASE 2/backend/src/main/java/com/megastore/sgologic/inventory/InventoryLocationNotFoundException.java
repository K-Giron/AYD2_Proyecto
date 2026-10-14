package com.megastore.sgologic.inventory;

import java.util.UUID;

public class InventoryLocationNotFoundException extends RuntimeException {

    public InventoryLocationNotFoundException(UUID locationId) {
        super("No existe una ubicacion con id " + locationId);
    }

    public InventoryLocationNotFoundException(String locationCode) {
        super("No existe una ubicacion con codigo " + locationCode);
    }
}
