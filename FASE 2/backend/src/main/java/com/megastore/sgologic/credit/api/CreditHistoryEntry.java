package com.megastore.sgologic.credit.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Un hecho del historial del cliente. {@code sourceReference} identifica el
 * hecho en el sistema de origen y es la llave de idempotencia.
 */
public record CreditHistoryEntry(
        UUID customerId,
        String sourceReference,
        CreditHistoryType type,
        Instant occurredAt,
        BigDecimal amount,
        Boolean onTime,
        boolean critical
) {
}
