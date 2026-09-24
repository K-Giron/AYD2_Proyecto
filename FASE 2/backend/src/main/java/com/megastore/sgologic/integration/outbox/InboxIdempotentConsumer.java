package com.megastore.sgologic.integration.outbox;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * Implementa el libro idempotente con una transacción propia
 * ({@code REQUIRES_NEW}). El registro usa {@code INSERT ... ON CONFLICT DO
 * NOTHING}, de modo que un reenvío del broker devuelve {@code false} sin
 * lanzar excepción ni contaminar la transacción del consumidor.
 *
 * <p>Limitación conocida: el registro ocurre antes del efecto de negocio; si
 * el consumidor falla después de registrarse, el reintento no volverá a
 * ejecutarlo. Para el MVP los consumidores se limitan a proyectar datos y la
 * política de reproceso es manual desde la bandeja de fallidos.</p>
 */
@Service
public class InboxIdempotentConsumer implements IdempotentConsumer {

    private final InboxEventRepository repository;
    private final Clock clock;

    public InboxIdempotentConsumer(InboxEventRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean registerProcessed(String consumer, UUID eventId) {
        if (consumer == null || consumer.isBlank() || eventId == null) {
            throw new IllegalArgumentException("consumer y eventId son obligatorios.");
        }
        int inserted = repository.tryRegister(UUID.randomUUID(), consumer, eventId, clock.instant());
        return inserted == 1;
    }
}
