package com.megastore.sgologic.onlinestore;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Envío a POS-ONLINE tal como lo entrega el central (GET /api/v1/store-sync/POS-ONLINE/receipts). */
record IncomingReceipt(String folio, String originFolio, String origin, UUID articleId, String sku, String name,
                       String description, BigDecimal price, int quantity, Instant occurredAt) {
}
