package com.megastore.inventoryservice.stock;

public enum StockMovementType {
    ENTRADA,
    SALIDA,
    /** Baja de existencia sin destino (merma, daño, corrección de conteo); motivo obligatorio. */
    AJUSTE
}
