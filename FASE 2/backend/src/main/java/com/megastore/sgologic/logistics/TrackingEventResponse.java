package com.megastore.sgologic.logistics;

import java.time.Instant;
import java.util.UUID;

public record TrackingEventResponse(UUID id, ShipmentStatus status, String source, Instant occurredAt) {

    static TrackingEventResponse from(ShipmentTrackingEvent event) {
        return new TrackingEventResponse(event.getId(), event.getStatus(), event.getSource(), event.getOccurredAt());
    }
}
