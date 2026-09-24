package com.megastore.sgologic.integration;

public enum PosDeliveryStatus {
    PENDING,
    PENDING_RETRY,
    ACKNOWLEDGED,
    REJECTED,
    DEAD_LETTER
}
