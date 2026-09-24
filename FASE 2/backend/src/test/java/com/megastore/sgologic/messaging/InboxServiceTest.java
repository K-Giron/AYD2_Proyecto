package com.megastore.sgologic.messaging;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InboxServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-23T23:00:00Z");

    @Test
    void processesAndMarksAnEventOnlyOnceForEachConsumer() {
        InboxMessageRepository repository = mock(InboxMessageRepository.class);
        InboxService service = new InboxService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
        BusinessEventEnvelope event = event();
        Runnable processing = mock(Runnable.class);
        when(repository.existsById(any())).thenReturn(false);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.processOnce("query-v1", event, processing)).isTrue();

        verify(processing).run();
        verify(repository, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void ignoresARepeatedEventWithoutExecutingItsSideEffect() {
        InboxMessageRepository repository = mock(InboxMessageRepository.class);
        InboxService service = new InboxService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
        Runnable processing = mock(Runnable.class);
        when(repository.existsById(any())).thenReturn(true);

        assertThat(service.processOnce("query-v1", event(), processing)).isFalse();

        verify(processing, never()).run();
        verify(repository, never()).save(any());
    }

    private BusinessEventEnvelope event() {
        UUID orderId = UUID.randomUUID();
        return new BusinessEventEnvelope(UUID.randomUUID(), "ORDER_STATUS_CHANGED", "ORDER",
                orderId, UUID.randomUUID(), NOW, Map.of("newStatus", "CONFIRMED"));
    }
}
