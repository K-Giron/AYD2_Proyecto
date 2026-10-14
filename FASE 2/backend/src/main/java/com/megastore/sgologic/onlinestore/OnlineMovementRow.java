package com.megastore.sgologic.onlinestore;

import java.time.Instant;
import java.util.UUID;

/** Línea del kárdex de la tienda online (ver {@code InventoryLocationActivityService.LocationActivity}). */
public record OnlineMovementRow(String kind, UUID productId, String sku, String name, int quantity, String reason,
                                String status, UUID orderId, Instant occurredAt, Instant expiresAt) {
}
