package com.megastore.sgologic.integration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "erp_batches")
public class ErpBatch {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false, length = 8)
    private ErpExportFormat format;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private ErpBatchStatus status;

    @Column(name = "sale_count", nullable = false)
    private int saleCount;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "acknowledgement", length = 128)
    private String acknowledgement;

    @Column(name = "failure_message", length = 500)
    private String failureMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ErpBatch() {
    }

    static ErpBatch create(ErpExportFormat format, String payload, int saleCount, Instant now) {
        ErpBatch batch = new ErpBatch();
        batch.id = UUID.randomUUID();
        batch.format = format;
        batch.payload = payload;
        batch.saleCount = saleCount;
        batch.status = saleCount == 0 ? ErpBatchStatus.EMPTY : ErpBatchStatus.PENDING;
        batch.createdAt = now;
        batch.updatedAt = now;
        return batch;
    }

    void markAttempt(Instant now) { attempts += 1; updatedAt = now; }
    void markDelivered(String acknowledgement, Instant now) {
        status = ErpBatchStatus.DELIVERED;
        this.acknowledgement = acknowledgement;
        failureMessage = null;
        deliveredAt = now;
        updatedAt = now;
    }
    void markRejected(String reason, Instant now) {
        status = ErpBatchStatus.REJECTED;
        failureMessage = reason;
        updatedAt = now;
    }
    void markPendingRetry(String reason, Instant now) {
        status = ErpBatchStatus.PENDING_RETRY;
        failureMessage = reason;
        updatedAt = now;
    }
    /** Finanzas reabre un lote agotado (el ERP ya volvió): se reintenta desde cero con las mismas ventas. */
    void reopenForRetry(Instant now) {
        status = ErpBatchStatus.PENDING_RETRY;
        attempts = 0;
        updatedAt = now;
    }
    void markDeadLetter(String reason, Instant now) {
        status = ErpBatchStatus.DEAD_LETTER;
        failureMessage = reason;
        updatedAt = now;
    }

    public UUID getId() { return id; }
    public ErpExportFormat getFormat() { return format; }
    public String getPayload() { return payload; }
    public ErpBatchStatus getStatus() { return status; }
    public int getSaleCount() { return saleCount; }
    public int getAttempts() { return attempts; }
    public String getAcknowledgement() { return acknowledgement; }
    public String getFailureMessage() { return failureMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeliveredAt() { return deliveredAt; }
}
