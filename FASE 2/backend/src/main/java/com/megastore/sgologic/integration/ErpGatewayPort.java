package com.megastore.sgologic.integration;

import java.util.UUID;

interface ErpGatewayPort {
    DeliveryResult deliver(UUID batchId, ErpExportFormat format, String payload);

    record DeliveryResult(boolean accepted, String acknowledgement, String rejectionReason) {
    }

    class ErpTimeoutException extends RuntimeException {
        ErpTimeoutException(String message) { super(message); }
    }

    class ErpUnavailableException extends RuntimeException {
        ErpUnavailableException(String message) { super(message); }
    }
}
