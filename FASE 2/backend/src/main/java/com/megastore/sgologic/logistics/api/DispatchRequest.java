package com.megastore.sgologic.logistics.api;

import java.util.UUID;

/** Solicitud pública de despacho consumida por la Saga de Orders. */
public record DispatchRequest(
        UUID orderId,
        String origin,
        String originCode,
        String destination,
        UUID correlationId,
        String idempotencyKey
) {
}
