package com.megastore.sgologic.inventory;

import java.util.UUID;

/**
 * Salida de inventario que excede las unidades disponibles (existencia no
 * reservada) en ese producto/ubicación. Distinta de
 * {@link InsufficientStockException}, que aplica al flujo de reserva.
 */
public class InsufficientOnHandStockException extends RuntimeException {

    public InsufficientOnHandStockException(UUID productId, UUID locationId, int requestedQuantity) {
        super("No hay suficiente existencia disponible del producto " + productId + " en la ubicación "
                + locationId + " para registrar una salida de " + requestedQuantity + " unidad(es)");
    }
}
