package com.megastore.sgologic.inventory;

import java.util.UUID;

/** Línea mínima que Orders entrega al puerto público de CDU-02. */
public record InventoryReservationLine(UUID productId, int quantity) {
}
