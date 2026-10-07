package com.megastore.inventoryservice.stock;

/** Datos mínimos de artículo para mostrar en la respuesta de un movimiento. */
public record ArticleSummary(String sku, String name) {
}
