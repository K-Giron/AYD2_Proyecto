package com.megastore.sgologic.inventorysync;

import java.util.UUID;

/**
 * Línea de una orden confirmada, en la forma mínima que necesita el
 * almacén central — desacoplada de {@code orders.OrderLine} para que el
 * puerto no dependa del tipo interno de ese módulo.
 */
public record SoldLine(UUID productId, String productName, int quantity) {
}
