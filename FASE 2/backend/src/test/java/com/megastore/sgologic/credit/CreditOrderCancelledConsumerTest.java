package com.megastore.sgologic.credit;

import com.megastore.sgologic.messaging.BusinessEventEnvelope;
import com.megastore.sgologic.messaging.InboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class CreditOrderCancelledConsumerTest {

    private InboxService inbox;
    private CreditLifecycleService lifecycle;
    private CreditOrderCancelledConsumer consumer;

    @BeforeEach
    void setUp() {
        inbox = mock(InboxService.class);
        lifecycle = mock(CreditLifecycleService.class);
        doAnswer(call -> {
            ((Runnable) call.getArgument(2)).run();
            return true;
        }).when(inbox).processOnce(any(), any(), any());
        consumer = new CreditOrderCancelledConsumer(inbox, lifecycle);
    }

    @Test
    void cancelledOrderTriggersTheCreditCompensation() {
        UUID orderId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();

        consumer.consume(event(orderId, correlationId, "CANCELLED"));

        verify(lifecycle).cancelForOrder(orderId, correlationId);
    }

    @Test
    void otherOrderStatusChangesAreIgnored() {
        consumer.consume(event(UUID.randomUUID(), UUID.randomUUID(), "CONFIRMED"));

        verify(lifecycle, never()).cancelForOrder(any(), any());
        verify(inbox, never()).processOnce(eq(CreditOrderCancelledConsumer.CONSUMER_NAME), any(), any());
    }

    @Test
    void incompatibleEventsAreRejected() {
        BusinessEventEnvelope wrong = new BusinessEventEnvelope(UUID.randomUUID(), "SHIPMENT_STATUS_CHANGED",
                "SHIPMENT", UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Map.of("newStatus", "CANCELLED"));

        assertThatThrownBy(() -> consumer.consume(wrong)).isInstanceOf(IllegalArgumentException.class);
    }

    private static BusinessEventEnvelope event(UUID orderId, UUID correlationId, String newStatus) {
        return new BusinessEventEnvelope(UUID.randomUUID(), "ORDER_STATUS_CHANGED", "ORDER", orderId,
                correlationId, Instant.now(), Map.of("newStatus", newStatus));
    }
}
