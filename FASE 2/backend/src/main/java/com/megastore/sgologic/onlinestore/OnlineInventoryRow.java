package com.megastore.sgologic.onlinestore;

import java.util.UUID;

/**
 * Existencia de la tienda online por producto: en mano y reservado en
 * CD-WEB, disponible para vender, en tránsito (enviado por el central y
 * aún sin confirmar) y si está publicado en la web.
 */
public record OnlineInventoryRow(UUID productId, String sku, String name, int onHand, int reserved, int available,
                                 int inTransit, boolean inCatalog, boolean published) {
}
