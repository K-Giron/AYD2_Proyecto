package com.megastore.inventoryservice.stock;

import java.time.Instant;
import java.util.UUID;

public record StockResponse(
        UUID articleId,
        String sku,
        String name,
        int quantityOnHand,
        UUID warehouseId,
        String warehouseCode,
        String warehouseName,
        Instant updatedAt
) {
    static StockResponse from(StockService.StockBalance balance) {
        return new StockResponse(
                balance.article().getId(),
                balance.article().getSku(),
                balance.article().getName(),
                balance.stock().getQuantityOnHand(),
                balance.warehouse().id(),
                balance.warehouse().code(),
                balance.warehouse().name(),
                balance.stock().getUpdatedAt());
    }
}
