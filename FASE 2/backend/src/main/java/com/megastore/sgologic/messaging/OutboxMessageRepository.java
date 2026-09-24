package com.megastore.sgologic.messaging;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface OutboxMessageRepository extends JpaRepository<OutboxMessage, UUID> {
    Optional<OutboxMessage> findByAggregateIdAndEventTypeAndIdempotencyKey(
            UUID aggregateId, String eventType, String idempotencyKey);

    List<OutboxMessage> findTop50ByStatusAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
            OutboxStatus status, Instant availableAt);
}
