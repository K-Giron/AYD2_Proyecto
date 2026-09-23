package com.megastore.sgologic.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Entrada del puerto de reserva de inventario que consumirá la Saga
 * (ORD-03, {@code saga-required-ports.md}): {@code orderId}, línea de
 * producto/cantidad y ubicación. {@code correlationId} e
 * {@code idempotencyKey} llegan por cabecera HTTP, igual que en LOG-01.
 */
public record CreateReservationRequest(
        @NotNull UUID orderId,
        @NotNull UUID productId,
        @NotNull UUID locationId,
        @Min(1) int quantity
) {
}
