package com.megastore.sgologic.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;

@Service
@ConditionalOnProperty(name = "sgo.messaging.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final int MAXIMUM_ATTEMPTS = 5;

    private final OutboxMessageRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final Clock clock;

    public OutboxPublisher(OutboxMessageRepository repository, RabbitTemplate rabbitTemplate, Clock clock) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${sgo.messaging.outbox.poll-interval-ms:1000}")
    @Transactional
    public void publishPending() {
        repository.findTop50ByStatusAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
                OutboxStatus.PENDING, clock.instant()).forEach(this::publish);
    }

    private void publish(OutboxMessage message) {
        try {
            rabbitTemplate.convertAndSend(OutboxRabbitConfig.BUSINESS_EXCHANGE,
                    routingKey(message), envelope(message));
            message.markPublished(clock.instant());
        } catch (AmqpException exception) {
            Duration backoff = Duration.ofSeconds(1L << Math.min(message.getAttempts(), 5));
            message.registerFailure(exception.getMessage(), clock.instant().plus(backoff), MAXIMUM_ATTEMPTS);
            LOGGER.warn("No se pudo publicar Outbox {} (intento {}): {}",
                    message.getId(), message.getAttempts(), exception.getMessage());
        }
        repository.save(message);
    }

    private String routingKey(OutboxMessage message) {
        return message.getAggregateType().toLowerCase() + "." + message.getEventType().toLowerCase();
    }

    private BusinessEventEnvelope envelope(OutboxMessage message) {
        return new BusinessEventEnvelope(message.getId(), message.getEventType(),
                message.getAggregateType(), message.getAggregateId(), message.getCorrelationId(),
                message.getCreatedAt(), message.getPayload());
    }
}
