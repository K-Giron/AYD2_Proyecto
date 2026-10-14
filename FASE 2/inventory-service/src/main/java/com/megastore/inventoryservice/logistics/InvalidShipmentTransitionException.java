package com.megastore.inventoryservice.logistics;

public class InvalidShipmentTransitionException extends RuntimeException {

    public InvalidShipmentTransitionException(String folio, ShipmentStatus current, ShipmentStatus requested) {
        super("El envío " + folio + " está en " + current + " y no puede pasar directo a " + requested);
    }
}
