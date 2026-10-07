package com.megastore.inventoryservice.logistics;

import com.megastore.inventoryservice.pointofsale.PointOfSaleResponse;

import java.time.Instant;
import java.util.UUID;

public record ShipmentResponse(
        UUID id,
        String folio,
        UUID movementId,
        UUID pointOfSaleId,
        String pointOfSaleCode,
        String pointOfSaleName,
        String warehouseCode,
        String warehouseName,
        ShipmentStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    static ShipmentResponse from(Shipment shipment, PointOfSaleResponse pointOfSale) {
        return new ShipmentResponse(shipment.getId(), shipment.getFolio(), shipment.getMovementId(),
                pointOfSale.id(), pointOfSale.code(), pointOfSale.name(),
                pointOfSale.warehouseCode(), pointOfSale.warehouseName(),
                shipment.getStatus(), shipment.getCreatedAt(), shipment.getUpdatedAt());
    }
}
