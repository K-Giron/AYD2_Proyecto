package com.megastore.inventoryservice.stock;

public class PointOfSaleRequiredException extends RuntimeException {

    public PointOfSaleRequiredException() {
        super("Una salida requiere indicar el punto de venta destino.");
    }
}
