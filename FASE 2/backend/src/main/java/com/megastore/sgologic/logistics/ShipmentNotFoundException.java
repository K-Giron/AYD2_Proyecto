package com.megastore.sgologic.logistics;

import java.util.UUID;

public class ShipmentNotFoundException extends RuntimeException {

    public ShipmentNotFoundException(UUID shipmentId) {
        super("No existe un despacho con id " + shipmentId);
    }
}
