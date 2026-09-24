package com.megastore.sgologic.messaging;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** Ejecuta cada evento una sola vez por consumidor dentro de la transacción local del destino. */
@Service
public class InboxService {

    private final InboxMessageRepository repository;
    private final Clock clock;

    public InboxService(InboxMessageRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public boolean processOnce(String consumerName, BusinessEventEnvelope event, Runnable processing) {
        InboxMessageId id = new InboxMessageId(consumerName, event.eventId());
        if (repository.existsById(id)) return false;

        InboxMessage message = repository.save(InboxMessage.received(consumerName, event, clock.instant()));
        processing.run();
        message.markProcessed(clock.instant());
        repository.save(message);
        return true;
    }
}
