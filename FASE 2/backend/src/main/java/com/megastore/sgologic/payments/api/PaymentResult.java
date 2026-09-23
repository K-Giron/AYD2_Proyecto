package com.megastore.sgologic.payments.api;

import java.time.Instant;
import java.util.UUID;

/**
 * Resultado mínimo de una autorización de pago según
 * {@code docs/contracts/orders/saga-required-ports.md}: identificador del
 * intento, estado, referencia del proveedor e instante del resultado.
 *
 * <p>Estados posibles: {@code PENDING}, {@code APPROVED}, {@code REJECTED} e
 * {@code INDETERMINATE}. Un timeout nunca se reporta como aprobado
 * (regla transversal del contrato).</p>
 */
public record PaymentResult(
        UUID paymentAttemptId,
        String status,
        String providerReference,
        Instant processedAt
) {
}
