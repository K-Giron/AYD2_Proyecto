package com.megastore.inventoryservice.storesync;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entrada a una tienda (folio POS-ENT) con los datos del artículo que la tienda necesita para venderlo.
 * {@code originFolio}: el folio que la tienda escribe al recibir (SAL-… del central o TRA-… de otra tienda);
 * {@code origin}: "CENTRAL" o el código de la tienda que trasladó.
 */
public record StoreReceiptResponse(String folio, String originFolio, String origin, UUID articleId, String sku, String name,
                                   String description, BigDecimal price, int quantity, Instant occurredAt) {
}
