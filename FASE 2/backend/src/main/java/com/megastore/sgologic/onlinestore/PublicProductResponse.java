package com.megastore.sgologic.onlinestore;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Producto tal como lo ve el cliente en la web. {@code available} =
 * existencia de la tienda online menos lo reservado por otras órdenes,
 * la misma que la reserva atómica valida al pagar.
 */
public record PublicProductResponse(UUID id, String sku, String name, String description, String category,
                                    BigDecimal price, String image, int available) {
}
