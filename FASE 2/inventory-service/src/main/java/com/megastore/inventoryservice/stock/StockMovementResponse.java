package com.megastore.inventoryservice.stock;

import com.megastore.inventoryservice.pointofsale.PointOfSaleResponse;
import com.megastore.inventoryservice.warehouse.WarehouseResponse;

import java.time.Instant;
import java.util.UUID;

public record StockMovementResponse(
        UUID movementId,
        String folio,
        StockMovementType type,
        UUID articleId,
        String articleSku,
        String articleName,
        int quantity,
        String reason,
        UUID pointOfSaleId,
        String pointOfSaleCode,
        String pointOfSaleName,
        /** Almacén de origen del movimiento (de dónde salió/entró, siempre el central). */
        String sourceWarehouseCode,
        String sourceWarehouseName,
        /** Almacén propio del punto de venta destino; solo aplica a salidas. */
        String warehouseCode,
        int quantityOnHandAfter,
        String externalReference,
        Instant occurredAt
) {
    static StockMovementResponse from(StockMovement movement, ArticleSummary article, int quantityOnHandAfter,
                                       PointOfSaleResponse pointOfSale, WarehouseResponse sourceWarehouse) {
        return new StockMovementResponse(
                movement.getId(),
                movement.getFolio(),
                movement.getType(),
                movement.getArticleId(),
                article.sku(),
                article.name(),
                movement.getQuantity(),
                movement.getReason(),
                pointOfSale == null ? null : pointOfSale.id(),
                pointOfSale == null ? null : pointOfSale.code(),
                pointOfSale == null ? null : pointOfSale.name(),
                sourceWarehouse.code(),
                sourceWarehouse.name(),
                pointOfSale == null ? null : pointOfSale.warehouseCode(),
                quantityOnHandAfter,
                movement.getExternalReference(),
                movement.getOccurredAt());
    }
}
