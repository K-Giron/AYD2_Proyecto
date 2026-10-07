package com.megastore.inventoryservice.posstock;

import java.time.Instant;
import java.util.UUID;

public record PosStockResponse(
        UUID pointOfSaleId,
        String pointOfSaleCode,
        String pointOfSaleName,
        UUID articleId,
        String articleSku,
        String articleName,
        int quantityOnHand,
        Instant updatedAt
) {
}
