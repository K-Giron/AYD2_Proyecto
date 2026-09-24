package com.megastore.sgologic.messaging;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class OutboxService {

    private final OutboxMessageRepository repository;
    private final Clock clock;

    public OutboxService(OutboxMessageRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** Se une a la transacción de negocio activa para evitar la doble escritura. */
    @Transactional
    public OutboxMessage record(OutboxCommand command) {
        return repository.findByAggregateIdAndEventTypeAndIdempotencyKey(
                        command.aggregateId(), command.eventType(), command.idempotencyKey())
                .orElseGet(() -> repository.save(OutboxMessage.pending(command, clock.instant())));
    }
}
