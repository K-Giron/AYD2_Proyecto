package com.megastore.sgologic.logistics;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Cuerpo de {@code POST /api/v1/shipments}, con la forma acordada en
 * {@code saga-required-ports.md} para el puerto de despacho: {@code orderId,
 * origen, destino}. El {@code correlationId} viaja en {@code X-Correlation-Id}
 * y la idempotencia en {@code Idempotency-Key} (checklist de entrega).
 */
public record CreateShipmentRequest(
        @NotNull UUID orderId,
        @NotNull DispatchOrigin origin,
        @NotBlank String destination
) {
}
