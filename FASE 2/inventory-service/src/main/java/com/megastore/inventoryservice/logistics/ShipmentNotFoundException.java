package com.megastore.inventoryservice.logistics;

public class ShipmentNotFoundException extends RuntimeException {

    public ShipmentNotFoundException(String folio) {
        super("No existe un envío con folio " + folio);
    }
}
