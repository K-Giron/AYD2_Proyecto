package com.megastore.sgologic.inventory;

import java.util.UUID;

/**
 * INV-04 / CDU-02: no hay ninguna ubicación (centro de distribución o
 * tienda) con disponibilidad suficiente para el producto solicitado, tras
 * intentar todas las candidatas conforme al criterio de ADR-014.
 */
public class NoAvailableOriginException extends RuntimeException {

    public NoAvailableOriginException(UUID productId, int requestedQuantity) {
        super("Ninguna ubicación tiene disponibilidad suficiente del producto " + productId
                + " para reservar " + requestedQuantity + " unidad(es)");
    }
}
