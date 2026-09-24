package com.megastore.sgologic.integration.outbox;

/** Estado de publicación de un evento saliente (BB-04, ADR-002). */
public enum OutboxEventStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
