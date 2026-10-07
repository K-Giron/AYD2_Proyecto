package com.megastore.storeservice.inventory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Recepción tal como la entrega el central (GET /api/v1/store-sync/{pos}/receipts).
 * {@code origin}: "CENTRAL" o el código de la tienda que trasladó.
 */
public record IncomingReceipt(String folio, String originFolio, String origin, UUID articleId, String sku,
                              String name, String description, BigDecimal price, int quantity, Instant occurredAt) {
}
