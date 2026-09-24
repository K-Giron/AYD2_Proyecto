package com.megastore.sgologic.messaging;

public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    DEAD_LETTER
}
