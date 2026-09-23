package com.megastore.sgologic.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * Escribe la evidencia sobre la propia consulta (acceso, exportación o
 * búsqueda fallida) en una transacción independiente
 * ({@code REQUIRES_NEW}). Así el registro sobrevive aunque la consulta
 * termine en error, como exige CDU-12 E1: la búsqueda fallida queda
 * registrada y la postcondición "la consulta queda registrada" se cumple.
 */
@Service
class AuditEvidenceWriter {

    private static final String ACCESS_AGGREGATE = "AuditEvidence";

    private final AuditEventRepository repository;
    private final AuditActorProvider actorProvider;
    private final Clock clock;

    AuditEvidenceWriter(AuditEventRepository repository, AuditActorProvider actorProvider, Clock clock) {
        this.repository = repository;
        this.actorProvider = actorProvider;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void recordQueryEvent(String action, String result, UUID aggregateId, UUID orderId, UUID correlationId) {
        repository.save(AuditEvent.create(UUID.randomUUID(), null, ACCESS_AGGREGATE, aggregateId, orderId,
                actorProvider.currentActor(), action, result, null, null, correlationId, null, clock.instant()));
    }
}
