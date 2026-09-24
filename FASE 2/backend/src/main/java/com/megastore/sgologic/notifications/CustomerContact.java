package com.megastore.sgologic.notifications;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Medio confirmado y preferencia local del cliente para CUN-11. */
@Entity
@Table(name = "customer_contacts")
class CustomerContact {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 16)
    private NotificationChannel channel;

    @Column(name = "destination_token", nullable = false, length = 200)
    private String destinationToken;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "preference_rank", nullable = false)
    private int preferenceRank;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CustomerContact() {
        // Requerido por JPA.
    }

    static CustomerContact verified(UUID customerId, NotificationChannel channel,
                                    String destinationToken, int preferenceRank, Instant now) {
        CustomerContact contact = new CustomerContact();
        contact.id = UUID.randomUUID();
        contact.customerId = customerId;
        contact.channel = channel;
        contact.destinationToken = destinationToken;
        contact.enabled = true;
        contact.preferenceRank = preferenceRank;
        contact.verifiedAt = now;
        contact.createdAt = now;
        contact.updatedAt = now;
        return contact;
    }

    NotificationChannel getChannel() {
        return channel;
    }

    String getDestinationToken() {
        return destinationToken;
    }
}
