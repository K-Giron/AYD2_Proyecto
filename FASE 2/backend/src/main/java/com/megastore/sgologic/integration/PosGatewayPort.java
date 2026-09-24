package com.megastore.sgologic.integration;

import java.util.UUID;

interface PosGatewayPort {
    DeliveryResult deliver(int storeNumber, UUID eventId, String eventType, UUID orderId,
                           String payload, UUID correlationId);

    record DeliveryResult(boolean accepted, String acknowledgement, String rejectionReason) {
    }

    class PosTimeoutException extends RuntimeException {
        PosTimeoutException(String message) { super(message); }
    }

    class PosUnavailableException extends RuntimeException {
        PosUnavailableException(String message) { super(message); }
    }
}
