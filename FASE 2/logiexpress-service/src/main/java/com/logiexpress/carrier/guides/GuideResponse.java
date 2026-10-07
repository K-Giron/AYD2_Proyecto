package com.logiexpress.carrier.guides;

import java.time.Instant;
import java.util.List;

public record GuideResponse(String trackingNumber, String merchantReference, String orderReference, String origin,
                            String destination, String pickupPoint, GuideStatus status, String courier,
                            Instant createdAt, Instant updatedAt, List<Event> events) {

    public record Event(GuideStatus status, String note, String actor, Instant occurredAt) {
    }

    static GuideResponse of(Guide guide, List<GuideEvent> events) {
        return new GuideResponse(guide.getTrackingNumber(), guide.getMerchantReference(), guide.getOrderReference(),
                guide.getOrigin(), guide.getDestination(), guide.getPickupPoint(), guide.getStatus(), guide.getCourier(),
                guide.getCreatedAt(), guide.getUpdatedAt(),
                events.stream().map(e -> new Event(e.getStatus(), e.getNote(), e.getActor(), e.getOccurredAt())).toList());
    }
}
