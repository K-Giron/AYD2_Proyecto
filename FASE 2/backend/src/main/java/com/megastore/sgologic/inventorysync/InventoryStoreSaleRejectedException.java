package com.megastore.sgologic.inventorysync;

/**
 * inventory-service rechazó la venta en tienda por un problema de datos
 * (la tienda no tiene esa existencia, o el punto de venta no existe). No
 * se reintenta: va a la bandeja de fallidos.
 */
public class InventoryStoreSaleRejectedException extends RuntimeException {

    public InventoryStoreSaleRejectedException(String message) {
        super(message);
    }
}
