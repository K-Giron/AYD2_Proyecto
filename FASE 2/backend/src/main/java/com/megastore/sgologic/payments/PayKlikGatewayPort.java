package com.megastore.sgologic.payments;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Contrato simulado de PayKlik entregado por BB-06 para PAY-01/PAY-02. */
public interface PayKlikGatewayPort {

    AuthorizationResult authorize(AuthorizationRequest request);

    record AuthorizationRequest(
            UUID orderId,
            BigDecimal amount,
            String currency,
            String paymentReference,
            UUID correlationId,
            String idempotencyKey
    ) {
    }

    record AuthorizationResult(
            UUID paymentAttemptId,
            boolean approved,
            String providerReference,
            String rejectionCode,
            Instant processedAt
    ) {
    }

    class PayKlikTimeoutException extends RuntimeException {
        public PayKlikTimeoutException(String message) {
            super(message);
        }
    }

    class PayKlikUnavailableException extends RuntimeException {
        public PayKlikUnavailableException(String message) {
            super(message);
        }
    }
}
