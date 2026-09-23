package com.megastore.sgologic.integration.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

interface InboxEventRepository extends JpaRepository<InboxEvent, UUID> {

    /**
     * Registra el evento de forma atómica. {@code ON CONFLICT DO NOTHING}
     * evita la excepción de restricción única y, con ella, que la
     * transacción quede marcada como rollback-only. Devuelve 1 si insertó y
     * 0 si el evento ya estaba registrado para ese consumidor.
     */
    @Modifying
    @Query(value = "INSERT INTO inbox_event (id, consumer, event_id, processed_at) "
            + "VALUES (:id, :consumer, :eventId, :processedAt) "
            + "ON CONFLICT (consumer, event_id) DO NOTHING",
            nativeQuery = true)
    int tryRegister(@Param("id") UUID id, @Param("consumer") String consumer,
                    @Param("eventId") UUID eventId, @Param("processedAt") Instant processedAt);
}
