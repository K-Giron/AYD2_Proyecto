package com.megastore.inventoryservice.pointofsale;

public class DuplicatePointOfSaleCodeException extends RuntimeException {

    public DuplicatePointOfSaleCodeException(String code) {
        super("Ya existe un punto de venta con código " + code);
    }
}
