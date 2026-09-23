package com.megastore.sgologic.fiscal;

import java.util.UUID;

/** Puerto fiscal propuesto para ORD-03 y materializado por FIS-02. */
public interface SatGatewayPort {

    SatResult submit(SatSubmission submission);

    record SatSubmission(
            UUID fiscalDocumentId,
            UUID orderId,
            String cui,
            String series,
            String xmlPayload,
            UUID correlationId,
            String idempotencyKey
    ) {
    }

    record SatResult(boolean approved, String authorization, String rejectionCode, String message) {
    }

    class SatTimeoutException extends RuntimeException {
        public SatTimeoutException(String message) { super(message); }
    }

    class SatUnavailableException extends RuntimeException {
        public SatUnavailableException(String message) { super(message); }
    }
}
