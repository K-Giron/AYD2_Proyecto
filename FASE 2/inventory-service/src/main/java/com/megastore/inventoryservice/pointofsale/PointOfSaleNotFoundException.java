package com.megastore.inventoryservice.pointofsale;

import java.util.UUID;

public class PointOfSaleNotFoundException extends RuntimeException {

    public PointOfSaleNotFoundException(UUID pointOfSaleId) {
        super("No existe un punto de venta con id " + pointOfSaleId);
    }

    public PointOfSaleNotFoundException(String code) {
        super("No existe un punto de venta con código " + code);
    }
}
