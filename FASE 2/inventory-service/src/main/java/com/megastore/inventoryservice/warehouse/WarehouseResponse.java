package com.megastore.inventoryservice.warehouse;

import java.util.UUID;

public record WarehouseResponse(UUID id, String code, String name) {

    public static WarehouseResponse from(Warehouse warehouse) {
        return new WarehouseResponse(warehouse.getId(), warehouse.getCode(), warehouse.getName());
    }
}
