package com.megastore.sgologic.onlinestore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "online_store_sync_log")
public class OnlineSyncLogEntry {

    @Id
    private UUID id;

    @Column(nullable = false, length = 8)
    private String direction;

    @Column(nullable = false, length = 16)
    private String kind;

    @Column(length = 64)
    private String reference;

    @Column(nullable = false, length = 8)
    private String result;

    @Column(nullable = false, length = 500)
    private String detail;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected OnlineSyncLogEntry() {
    }

    static OnlineSyncLogEntry of(String direction, String kind, String reference, boolean ok, String detail,
                                 Instant now) {
        OnlineSyncLogEntry entry = new OnlineSyncLogEntry();
        entry.id = UUID.randomUUID();
        entry.direction = direction;
        entry.kind = kind;
        entry.reference = reference == null ? null : reference.substring(0, Math.min(reference.length(), 64));
        entry.result = ok ? "OK" : "ERROR";
        entry.detail = detail.substring(0, Math.min(detail.length(), 500));
        entry.occurredAt = now;
        return entry;
    }

    public UUID getId() { return id; }
    public String getDirection() { return direction; }
    public String getKind() { return kind; }
    public String getReference() { return reference; }
    public String getResult() { return result; }
    public String getDetail() { return detail; }
    public Instant getOccurredAt() { return occurredAt; }
}
