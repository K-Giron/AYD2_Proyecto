package com.megastore.sgologic.integration.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Reclama un lote de eventos pendientes con {@code FOR UPDATE SKIP LOCKED}:
     * si varios dispatchers compiten, cada uno toma filas distintas y ningún
     * evento se publica dos veces. Debe invocarse dentro de una transacción.
     */
    @Query(value = "SELECT * FROM outbox_event "
            + "WHERE status = 'PENDING' AND next_attempt_at <= :now "
            + "ORDER BY created_at ASC LIMIT :batchSize FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<OutboxEvent> lockPendingBatch(@Param("now") Instant now, @Param("batchSize") int batchSize);

    long countByStatus(OutboxEventStatus status);

    long countByAggregateId(UUID aggregateId);
}
