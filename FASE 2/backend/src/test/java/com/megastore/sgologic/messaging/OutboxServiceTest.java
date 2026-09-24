package com.megastore.sgologic.messaging;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-23T22:30:00Z");

    @Test
    void recordsOnePendingMessageAndDeduplicatesTheBusinessEvent() {
        OutboxMessageRepository repository = mock(OutboxMessageRepository.class);
        OutboxService service = new OutboxService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
        UUID aggregateId = UUID.randomUUID();
        OutboxCommand command = new OutboxCommand("ORDER", aggregateId, "ORDER_CONFIRMED",
                Map.of("status", "CONFIRMED"), UUID.randomUUID(), "checkout-1");
        when(repository.findByAggregateIdAndEventTypeAndIdempotencyKey(
                aggregateId, "ORDER_CONFIRMED", "checkout-1"))
                .thenReturn(Optional.empty());
        when(repository.save(any(OutboxMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OutboxMessage created = service.record(command);

        assertThat(created.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(created.getCreatedAt()).isEqualTo(NOW);
        assertThat(created.getPayload()).containsEntry("status", "CONFIRMED");
        verify(repository).save(created);

        when(repository.findByAggregateIdAndEventTypeAndIdempotencyKey(
                aggregateId, "ORDER_CONFIRMED", "checkout-1"))
                .thenReturn(Optional.of(created));
        OutboxMessage repeated = service.record(command);
        assertThat(repeated).isSameAs(created);
        verify(repository, org.mockito.Mockito.times(1)).save(any());
    }
}
