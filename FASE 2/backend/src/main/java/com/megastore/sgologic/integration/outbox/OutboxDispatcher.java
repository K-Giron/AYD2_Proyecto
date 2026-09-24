package com.megastore.sgologic.integration.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Publica en RabbitMQ los eventos pendientes del Outbox (BB-04).
 *
 * <p>El lote se reclama con {@code FOR UPDATE SKIP LOCKED}: varios
 * dispatchers pueden convivir sin publicar el mismo evento dos veces. Si la
 * publicación falla, el evento queda {@code PENDING} con el siguiente
 * backoff; al agotar los intentos pasa a {@code FAILED} y permanece visible
 * en la tabla para reproceso controlado.</p>
 */
@Component
public class OutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);

    private final OutboxEventRepository repository;
    private final OutboxProperties properties;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OutboxDispatcher(OutboxEventRepository repository, OutboxProperties properties,
                            RabbitTemplate rabbitTemplate, ObjectMapper objectMapper, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${sgo.outbox.poll-interval-ms:2000}")
    @Transactional
    public void dispatchPending() {
        if (!properties.isEnabled()) {
            return;
        }
        List<OutboxEvent> events = repository.lockPendingBatch(clock.instant(), properties.getBatchSize());
        for (OutboxEvent event : events) {
            try {
                rabbitTemplate.send(properties.getExchange(), event.getEventType(), toMessage(event));
                event.markPublished(clock.instant());
                log.info("Evento publicado: eventId={}, eventType={}, correlationId={}",
                        event.getEventId(), event.getEventType(), event.getCorrelationId());
            } catch (Exception failure) {
                boolean retrying = event.registerFailure(failure.getMessage(), clock.instant(),
                        properties.getRetryDelays(), properties.getMaxAttempts());
                if (retrying) {
                    log.warn("Fallo al publicar eventId={} (intento {}); reintento programado: {}",
                            event.getEventId(), event.getAttempts(), failure.getMessage());
                } else {
                    log.error("Evento eventId={} agotó los intentos y quedó FAILED: {}",
                            event.getEventId(), failure.getMessage());
                }
            }
            repository.save(event);
        }
        if (!events.isEmpty()) {
            log.debug("Dispatcher Outbox procesó {} evento(s); pendientes={}, fallidos={}",
                    events.size(), repository.countByStatus(OutboxEventStatus.PENDING),
                    repository.countByStatus(OutboxEventStatus.FAILED));
        }
    }

    private Message toMessage(OutboxEvent event) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", event.getEventId());
        envelope.put("eventType", event.getEventType());
        envelope.put("eventVersion", event.getEventVersion());
        envelope.put("occurredAt", event.getCreatedAt());
        envelope.put("correlationId", event.getCorrelationId());
        envelope.put("causationId", event.getCausationId());
        envelope.put("aggregateType", event.getAggregateType());
        envelope.put("aggregateId", event.getAggregateId());
        envelope.put("payload", payloadNode(event.getPayload()));
        String json = objectMapper.writeValueAsString(envelope);
        return MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setContentEncoding(StandardCharsets.UTF_8.name())
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .setMessageId(event.getEventId().toString())
                .setHeader("X-Correlation-Id", event.getCorrelationId().toString())
                .setHeader("X-Event-Type", event.getEventType())
                .build();
    }

    private JsonNode payloadNode(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        return objectMapper.readTree(payload);
    }
}
