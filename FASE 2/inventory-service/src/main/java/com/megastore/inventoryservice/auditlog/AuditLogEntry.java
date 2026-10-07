package com.megastore.inventoryservice.auditlog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Traza de un evento del almacén central: qué pasó, con qué folio (si
 * aplica) y qué actor lo hizo. Es la bitácora que permite seguir un folio
 * desde que se registra en el servidor hasta que impacta el stock de un
 * punto de venta, o una cuenta desde que se crea hasta que se liquida.
 */
@Entity
@Table(name = "audit_log_entries")
public class AuditLogEntry {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "event_type", nullable = false, updatable = false, length = 48)
    private String eventType;

    @Column(name = "description", nullable = false, updatable = false, length = 500)
    private String description;

    @Column(name = "related_folio", updatable = false, length = 24)
    private String relatedFolio;

    @Column(name = "point_of_sale_id", updatable = false)
    private UUID pointOfSaleId;

    @Column(name = "actor", nullable = false, updatable = false, length = 160)
    private String actor;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected AuditLogEntry() {
        // Requerido por JPA.
    }

    static AuditLogEntry record(String eventType, String description, String relatedFolio, UUID pointOfSaleId,
                                 String actor, Instant now) {
        AuditLogEntry entry = new AuditLogEntry();
        entry.id = UUID.randomUUID();
        entry.eventType = eventType;
        entry.description = description;
        entry.relatedFolio = relatedFolio;
        entry.pointOfSaleId = pointOfSaleId;
        entry.actor = actor;
        entry.occurredAt = now;
        return entry;
    }

    public UUID getId() { return id; }
    public String getEventType() { return eventType; }
    public String getDescription() { return description; }
    public String getRelatedFolio() { return relatedFolio; }
    public UUID getPointOfSaleId() { return pointOfSaleId; }
    public String getActor() { return actor; }
    public Instant getOccurredAt() { return occurredAt; }
}
