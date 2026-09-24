package com.megastore.sgologic.integration.outbox;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * BB-04: comportamiento del dispatcher ante broker disponible y caído.
 * La caída no pierde el evento: aplica backoff y termina en {@code FAILED}.
 */
class OutboxDispatcherTest {

    private static final Instant NOW = Instant.parse("2026-09-23T10:00:00Z");

    private final OutboxEventRepository repository = mock(OutboxEventRepository.class);
    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final OutboxProperties properties = new OutboxProperties();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final OutboxDispatcher dispatcher =
            new OutboxDispatcher(repository, properties, rabbitTemplate, new ObjectMapper(), clock);

    @Test
    void publishesPendingEventAndMarksItPublished() {
        OutboxEvent event = newEvent();
        when(repository.lockPendingBatch(any(), anyInt())).thenReturn(List.of(event));

        dispatcher.dispatchPending();

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
        assertThat(event.getAttempts()).isZero();
        assertThat(event.getPublishedAt()).isEqualTo(NOW);
        verify(rabbitTemplate).send(eq("sgo.events"), eq("OrderCreated"), any(Message.class));
    }

    @Test
    void failedPublishRetriesWithBackoffThenMarksFailed() {
        OutboxEvent event = newEvent();
        when(repository.lockPendingBatch(any(), anyInt())).thenReturn(List.of(event));
        doThrow(new AmqpException("broker caído"))
                .when(rabbitTemplate).send(anyString(), anyString(), any(Message.class));

        dispatcher.dispatchPending();
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(2));

        dispatcher.dispatchPending();
        assertThat(event.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(8));

        dispatcher.dispatchPending();
        assertThat(event.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(32));

        dispatcher.dispatchPending();
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(event.getAttempts()).isEqualTo(4);
    }

    private OutboxEvent newEvent() {
        return OutboxEvent.create(UUID.randomUUID(), "Order", UUID.randomUUID(), "OrderCreated",
                1, "{\"orderId\":\"1\"}", UUID.randomUUID(), null, NOW);
    }
}
