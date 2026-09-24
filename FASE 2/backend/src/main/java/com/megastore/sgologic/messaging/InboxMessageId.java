package com.megastore.sgologic.messaging;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
class InboxMessageId implements Serializable {

    @Column(name = "consumer_name", nullable = false, updatable = false, length = 120)
    private String consumerName;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    protected InboxMessageId() {
    }

    InboxMessageId(String consumerName, UUID eventId) {
        this.consumerName = consumerName;
        this.eventId = eventId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof InboxMessageId that)) return false;
        return Objects.equals(consumerName, that.consumerName) && Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(consumerName, eventId);
    }
}
