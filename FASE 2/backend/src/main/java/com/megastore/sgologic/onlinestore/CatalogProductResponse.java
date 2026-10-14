package com.megastore.sgologic.onlinestore;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Ficha para la administración: datos publicables más la existencia real
 * de la tienda online (en mano, reservada por órdenes en curso, disponible).
 */
public record CatalogProductResponse(UUID id, String sku, String name, String description, String category,
                                     BigDecimal price, String image, boolean published, Instant publishedAt,
                                     int onHand, int reserved, int available, Instant updatedAt) {
}
