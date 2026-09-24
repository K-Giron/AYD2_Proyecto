package com.megastore.sgologic.inventory;

import java.util.List;
import java.util.UUID;

/** Puerto público consumido por la Saga para reservar todas las líneas de una orden. */
public interface InventoryReservationPort {
    List<InventoryReservationAllocation> reserveOrder(
            UUID orderId,
            List<InventoryReservationLine> lines,
            UUID correlationId,
            String idempotencyKey
    );
}
