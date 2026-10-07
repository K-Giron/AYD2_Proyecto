package com.logiexpress.carrier.guides;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "guide_events")
public class GuideEvent {

    @Id
    private UUID id;

    @Column(name = "guide_id", nullable = false)
    private UUID guideId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GuideStatus status;

    private String note;

    @Column(nullable = false)
    private String actor;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected GuideEvent() {
    }

    static GuideEvent of(UUID guideId, GuideStatus status, String note, String actor, Instant now) {
        GuideEvent event = new GuideEvent();
        event.id = UUID.randomUUID();
        event.guideId = guideId;
        event.status = status;
        event.note = note;
        event.actor = actor;
        event.occurredAt = now;
        return event;
    }

    public UUID getGuideId() { return guideId; }
    public GuideStatus getStatus() { return status; }
    public String getNote() { return note; }
    public String getActor() { return actor; }
    public Instant getOccurredAt() { return occurredAt; }
}
