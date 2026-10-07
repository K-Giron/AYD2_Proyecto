package com.megastore.inventoryservice.posstock;

/** ENTRADA: llega por folio. VENTA: venta en la tienda. TRASLADO: sale por folio hacia el central u otra tienda. */
public enum PosStockMovementType {
    ENTRADA,
    VENTA,
    TRASLADO
}
