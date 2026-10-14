package com.megastore.sgologic.notifications;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Cola de mensajes fallidos local a Notifications (CUN-11), simétrica a
 * {@code logistics.ShipmentDeadLetter} (LOG-03). Se usa mientras no exista
 * un DLQ transversal del broker para el trabajador de notificaciones.
 */
@Entity
@Table(name = "notification_dead_letters")
public class NotificationDeadLetter {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "notification_id", nullable = false, updatable = false)
    private UUID notificationId;

    @Column(name = "reason", nullable = false, length = 300)
    private String reason;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private Instant registeredAt;

    protected NotificationDeadLetter() {
        // Requerido por JPA.
    }

    private NotificationDeadLetter(UUID id, UUID notificationId, String reason, int attempts, Instant registeredAt) {
        this.id = id;
        this.notificationId = notificationId;
        this.reason = reason;
        this.attempts = attempts;
        this.registeredAt = registeredAt;
    }

    public static NotificationDeadLetter of(UUID notificationId, String reason, int attempts, Instant registeredAt) {
        return new NotificationDeadLetter(UUID.randomUUID(), notificationId, reason, attempts, registeredAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public String getReason() {
        return reason;
    }

    public int getAttempts() {
        return attempts;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }
}
