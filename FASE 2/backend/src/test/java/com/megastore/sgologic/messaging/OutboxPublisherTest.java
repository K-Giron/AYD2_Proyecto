package com.megastore.sgologic.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.net.ConnectException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxPublisherTest {

    private static final Instant NOW = Instant.parse("2026-09-23T22:30:00Z");

    @Test
    void marksMessagePublishedOnlyAfterRabbitAcceptsIt() {
        OutboxMessageRepository repository = mock(OutboxMessageRepository.class);
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        OutboxMessage message = pendingMessage();
        when(repository.findTop50ByStatusAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
                OutboxStatus.PENDING, NOW)).thenReturn(List.of(message));

        new OutboxPublisher(repository, rabbit, Clock.fixed(NOW, ZoneOffset.UTC)).publishPending();

        verify(rabbit).convertAndSend(anyString(), anyString(), any(Object.class));
        assertThat(message.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(message.getPublishedAt()).isEqualTo(NOW);
        verify(repository).save(message);
    }

    @Test
    void retainsPendingMessageAndSchedulesBackoffWhenRabbitFails() {
        OutboxMessageRepository repository = mock(OutboxMessageRepository.class);
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        OutboxMessage message = pendingMessage();
        when(repository.findTop50ByStatusAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
                OutboxStatus.PENDING, NOW)).thenReturn(List.of(message));
        doThrow(new AmqpConnectException(new ConnectException("broker down")))
                .when(rabbit).convertAndSend(anyString(), anyString(), any(Object.class));

        new OutboxPublisher(repository, rabbit, Clock.fixed(NOW, ZoneOffset.UTC)).publishPending();

        assertThat(message.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(message.getAttempts()).isEqualTo(1);
        assertThat(message.getAvailableAt()).isAfter(NOW);
        assertThat(message.getLastError()).contains("broker down");
        verify(repository).save(message);
    }

    @Test
    void publishesCreationEventsWhosePreviousStatusIsNull() {
        OutboxMessageRepository repository = mock(OutboxMessageRepository.class);
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("previousStatus", null);
        payload.put("newStatus", "CREATED");
        OutboxMessage message = OutboxMessage.pending(new OutboxCommand("ORDER", UUID.randomUUID(),
                "ORDER_STATUS_CHANGED", payload, UUID.randomUUID(), "creation-1"), NOW);
        when(repository.findTop50ByStatusAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
                OutboxStatus.PENDING, NOW)).thenReturn(List.of(message));

        new OutboxPublisher(repository, rabbit, Clock.fixed(NOW, ZoneOffset.UTC)).publishPending();

        assertThat(message.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        verify(rabbit).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    private OutboxMessage pendingMessage() {
        return OutboxMessage.pending(new OutboxCommand("ORDER", UUID.randomUUID(),
                "ORDER_STATUS_CHANGED", Map.of("status", "CONFIRMED"),
                UUID.randomUUID(), UUID.randomUUID().toString()), NOW);
    }
}
