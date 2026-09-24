package com.megastore.sgologic.query;

import com.megastore.sgologic.messaging.BusinessEventEnvelope;
import com.megastore.sgologic.messaging.InboxService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OrderStatusChangedConsumerTest {

    @Test
    void delegatesProjectionThroughTheIdempotentInbox() {
        InboxService inbox = mock(InboxService.class);
        OrderSummaryQueryService query = mock(OrderSummaryQueryService.class);
        OrderStatusChangedConsumer consumer = new OrderStatusChangedConsumer(inbox, query);
        UUID orderId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-09-23T23:00:00Z");
        BusinessEventEnvelope event = new BusinessEventEnvelope(UUID.randomUUID(),
                "ORDER_STATUS_CHANGED", "ORDER", orderId, UUID.randomUUID(), occurredAt,
                Map.of("newStatus", "CONFIRMED", "occurredAt", occurredAt.toString()));
        ArgumentCaptor<Runnable> action = ArgumentCaptor.forClass(Runnable.class);

        consumer.consume(event);

        verify(inbox).processOnce(eq(OrderStatusChangedConsumer.CONSUMER_NAME), eq(event), action.capture());
        action.getValue().run();
        verify(query).upsert(orderId, ProjectionSource.ORDER, "CONFIRMED", occurredAt);
    }

    @Test
    void rejectsAnEnvelopeWithoutTheRequiredStatus() {
        OrderStatusChangedConsumer consumer = new OrderStatusChangedConsumer(
                mock(InboxService.class), mock(OrderSummaryQueryService.class));
        BusinessEventEnvelope event = new BusinessEventEnvelope(UUID.randomUUID(),
                "ORDER_STATUS_CHANGED", "ORDER", UUID.randomUUID(), UUID.randomUUID(),
                Instant.now(), Map.of());

        assertThatThrownBy(() -> consumer.consume(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("newStatus");
    }
}
