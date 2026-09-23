package com.megastore.sgologic.integration.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.util.UUID;

/**
 * Implementa {@link PublishEvent}: serializa el payload y escribe el evento
 * en la misma transacción del cambio de negocio. La publicación real la hace
 * {@link OutboxDispatcher} después del commit.
 */
@Service
public class OutboxPublisher implements PublishEvent {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OutboxPublisher(OutboxEventRepository repository, ObjectMapper objectMapper, Clock clock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public UUID publish(OutboxEventRequest request) {
        UUID eventId = UUID.randomUUID();
        OutboxEvent event = OutboxEvent.create(
                eventId, request.aggregateType(), request.aggregateId(), request.eventType(),
                request.eventVersion(), serialize(request.payload()), request.correlationId(),
                request.causationId(), clock.instant());
        repository.save(event);
        log.info("Evento registrado en Outbox: eventId={}, eventType={}, aggregateId={}, correlationId={}",
                eventId, request.eventType(), request.aggregateId(), request.correlationId());
        return eventId;
    }

    private String serialize(Object payload) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JacksonException failure) {
            throw new IllegalArgumentException("El payload del evento no se puede serializar a JSON.", failure);
        }
    }
}
