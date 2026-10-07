package com.megastore.storeservice.posevents;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Evento de una orden web recibido desde SGO-Logic. */
@Entity
@Table(name = "pos_events")
public class PosEvent {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(nullable = false, length = 24)
    private String status;

    @Column(name = "pickup_store", length = 16)
    private String pickupStore;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    protected PosEvent() {
    }

    static PosEvent received(UUID eventId, String eventType, UUID orderId, String status, String pickupStore,
                             String payload, Instant now) {
        PosEvent event = new PosEvent();
        event.eventId = eventId;
        event.eventType = eventType;
        event.orderId = orderId;
        event.status = status;
        event.pickupStore = pickupStore;
        event.payload = payload;
        event.receivedAt = now;
        return event;
    }

    public UUID getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public UUID getOrderId() { return orderId; }
    public String getStatus() { return status; }
    public String getPickupStore() { return pickupStore; }
    public String getPayload() { return payload; }
    public Instant getReceivedAt() { return receivedAt; }
}
