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

    /** Recupera las asignaciones sin exponer entidades ni repositorios de Inventory. */
    List<InventoryReservationAllocation> findOrderReservations(UUID orderId);

    /** Convierte las reservas activas en consumo definitivo de inventario. */
    void confirmOrderReservations(UUID orderId);

    /** Libera, de forma idempotente, las reservas activas de una orden cancelada. */
    void releaseOrderReservations(UUID orderId);
}
