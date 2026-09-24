package com.megastore.sgologic.inventory;

import java.util.UUID;

/**
 * CDU-02 E1: no hay unidades suficientes disponibles en el momento de la
 * reserva (incluye el caso de la última unidad ya tomada por otra
 * solicitud concurrente). Distinta de {@link InventoryItemNotFoundException}:
 * aquí el producto y la ubicación sí existen, solo falta stock.
 */
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(UUID productId, UUID locationId, int requestedQuantity) {
        super("No hay suficiente disponibilidad del producto " + productId + " en la ubicación "
                + locationId + " para reservar " + requestedQuantity + " unidad(es)");
    }
}
