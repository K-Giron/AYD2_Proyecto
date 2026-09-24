package com.megastore.sgologic.inventory;

import java.util.UUID;

public class InventoryReservationNotFoundException extends RuntimeException {

    public InventoryReservationNotFoundException(UUID reservationId) {
        super("No existe una reserva con id " + reservationId);
    }
}
