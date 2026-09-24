package com.megastore.sgologic.integration.outbox;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BB-04: máquina de estados del evento saliente. Verifica el backoff
 * 2/8/32 s, el paso a {@code FAILED} al agotar los intentos y la marca de
 * publicación.
 */
class OutboxEventTest {

    private static final Instant NOW = Instant.parse("2026-09-23T10:00:00Z");
    private static final List<Duration> RETRY_DELAYS =
            List.of(Duration.ofSeconds(2), Duration.ofSeconds(8), Duration.ofSeconds(32));
    private static final int MAX_ATTEMPTS = 4;

    @Test
    void retriesWithBackoffAndFailsAfterMaxAttempts() {
        OutboxEvent event = newEvent();

        assertThat(event.registerFailure("broker caído", NOW, RETRY_DELAYS, MAX_ATTEMPTS)).isTrue();
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(2));

        assertThat(event.registerFailure("broker caído", NOW, RETRY_DELAYS, MAX_ATTEMPTS)).isTrue();
        assertThat(event.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(8));

        assertThat(event.registerFailure("broker caído", NOW, RETRY_DELAYS, MAX_ATTEMPTS)).isTrue();
        assertThat(event.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(32));

        assertThat(event.registerFailure("broker caído", NOW, RETRY_DELAYS, MAX_ATTEMPTS)).isFalse();
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(event.getAttempts()).isEqualTo(4);
        assertThat(event.getFailureMessage()).isEqualTo("broker caído");
    }

    @Test
    void markPublishedSetsTerminalState() {
        OutboxEvent event = newEvent();
        event.registerFailure("fallo previo", NOW, RETRY_DELAYS, MAX_ATTEMPTS);

        event.markPublished(NOW.plusSeconds(1));

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isEqualTo(NOW.plusSeconds(1));
        assertThat(event.getFailureMessage()).isNull();
    }

    private OutboxEvent newEvent() {
        return OutboxEvent.create(UUID.randomUUID(), "Order", UUID.randomUUID(), "OrderCreated",
                1, "{\"orderId\":\"1\"}", UUID.randomUUID(), null, NOW);
    }
}
