package com.megastore.sgologic.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Entrada del puerto de reserva de inventario que consumirá la Saga
 * (ORD-03, {@code saga-required-ports.md}): {@code orderId}, línea de
 * producto/cantidad. {@code correlationId} e {@code idempotencyKey} llegan
 * por cabecera HTTP, igual que en LOG-01.
 *
 * <p>{@code locationId} es opcional: el contrato del puerto no lo incluye
 * (la Saga no decide la ubicación). Si se omite, se aplica el algoritmo de
 * selección de origen de ADR-014/INV-04; si se envía, se reserva
 * exactamente ahí (útil para el canal tienda, que ya conoce su propia
 * ubicación).</p>
 */
public record CreateReservationRequest(
        @NotNull UUID orderId,
        @NotNull UUID productId,
        UUID locationId,
        @Min(1) int quantity
) {
}
