package com.megastore.sgologic.audit;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Acceso a la evidencia (AUD-01/AUD-02). Extiende {@link Repository}, no
 * {@code JpaRepository}: solo expone inserción y consultas, sin operaciones
 * de borrado ni de actualización, para que el registro sea inmutable desde
 * la aplicación.
 */
interface AuditEventRepository extends Repository<AuditEvent, UUID> {

    AuditEvent save(AuditEvent event);

    List<AuditEvent> findByOrderIdOrderByOccurredAtAsc(UUID orderId);

    List<AuditEvent> findByAggregateTypeAndAggregateIdOrderByOccurredAtAsc(String aggregateType, UUID aggregateId);

    long countByAggregateId(UUID aggregateId);
}
