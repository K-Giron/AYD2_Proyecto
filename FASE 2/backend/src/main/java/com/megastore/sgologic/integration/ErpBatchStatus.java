package com.megastore.sgologic.integration;

public enum ErpBatchStatus {
    EMPTY,
    PENDING,
    PENDING_RETRY,
    DELIVERED,
    REJECTED,
    DEAD_LETTER
}
