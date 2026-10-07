package com.megastore.inventoryservice.warehouse;

import java.util.UUID;

public class WarehouseNotFoundException extends RuntimeException {

    public WarehouseNotFoundException(UUID warehouseId) {
        super("No existe un almacén con id " + warehouseId);
    }
}
