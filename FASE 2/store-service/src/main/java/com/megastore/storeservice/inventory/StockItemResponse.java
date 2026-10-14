package com.megastore.storeservice.inventory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** {@code inTransit}: enviado por el central y aún no confirmado por la tienda; no se puede vender. */
public record StockItemResponse(UUID articleId, String sku, String name, String description, BigDecimal price,
                                int quantity, int inTransit, Instant updatedAt) {
}
