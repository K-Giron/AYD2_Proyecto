package com.megastore.inventoryservice.pointofsale;

import com.megastore.inventoryservice.warehouse.Warehouse;

import java.util.UUID;

public record PointOfSaleResponse(
        UUID id,
        String code,
        String name,
        UUID warehouseId,
        String warehouseCode,
        String warehouseName
) {
    static PointOfSaleResponse from(PointOfSale pointOfSale, Warehouse warehouse) {
        return new PointOfSaleResponse(pointOfSale.getId(), pointOfSale.getCode(), pointOfSale.getName(),
                warehouse.getId(), warehouse.getCode(), warehouse.getName());
    }
}
