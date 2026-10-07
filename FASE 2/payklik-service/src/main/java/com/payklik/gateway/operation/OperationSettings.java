package com.payklik.gateway.operation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "operation_settings")
public class OperationSettings {

    @Id
    private short id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OperationMode mode;

    @Column(name = "slow_latency_ms", nullable = false)
    private int slowLatencyMs;

    @Column(name = "updated_by", nullable = false)
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OperationSettings() {
    }

    void change(OperationMode mode, String updatedBy, Instant now) {
        this.mode = mode;
        this.updatedBy = updatedBy;
        this.updatedAt = now;
    }

    public OperationMode getMode() { return mode; }
    public int getSlowLatencyMs() { return slowLatencyMs; }
    public String getUpdatedBy() { return updatedBy; }
    public Instant getUpdatedAt() { return updatedAt; }
}
