package com.megastore.sgologic.payments;

import java.util.Optional;

/** Contrato público usado por el orquestador de órdenes. */
public interface PaymentProcessingPort {
    PaymentProcessingResult process(PaymentProcessingRequest request);

    Optional<PaymentProcessingResult> findByIdempotencyKey(String idempotencyKey);
}
