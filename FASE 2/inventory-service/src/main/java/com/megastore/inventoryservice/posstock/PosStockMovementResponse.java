package com.megastore.inventoryservice.posstock;

import java.time.Instant;
import java.util.UUID;

public record PosStockMovementResponse(
        UUID id,
        String folio,
        PosStockMovementType type,
        UUID pointOfSaleId,
        String pointOfSaleCode,
        UUID articleId,
        String articleSku,
        String articleName,
        int quantity,
        String originWarehouseFolio,
        String externalReference,
        Instant occurredAt
) {
}
