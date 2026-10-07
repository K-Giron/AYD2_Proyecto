package com.megastore.sgologic.query;

import com.megastore.sgologic.messaging.BusinessEventEnvelope;
import com.megastore.sgologic.messaging.InboxService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PaymentStatusChangedConsumerTest {

    private final InboxService inbox = mock(InboxService.class);
    private final OrderSummaryQueryService query = mock(OrderSummaryQueryService.class);
    private final PaymentStatusChangedConsumer consumer = new PaymentStatusChangedConsumer(inbox, query);

    @Test
    void reconciledPaymentReachesTheOrderSummary() {
        UUID orderId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-10-07T15:00:00Z");
        BusinessEventEnvelope event = new BusinessEventEnvelope(UUID.randomUUID(), "PaymentApproved", "PaymentAttempt",
                UUID.randomUUID(), UUID.randomUUID(), occurredAt,
                Map.of("orderId", orderId.toString(), "status", "APPROVED", "providerReference", "PK-00000002"));
        ArgumentCaptor<Runnable> action = ArgumentCaptor.forClass(Runnable.class);

        consumer.consume(event);

        verify(inbox).processOnce(eq(PaymentStatusChangedConsumer.CONSUMER_NAME), eq(event), action.capture());
        action.getValue().run();
        verify(query).upsert(orderId, ProjectionSource.PAYMENT, "APPROVED", occurredAt);
    }

    @Test
    void rejectsEventsThatAreNotPayments() {
        BusinessEventEnvelope event = new BusinessEventEnvelope(UUID.randomUUID(), "SHIPMENT_STATUS_CHANGED", "SHIPMENT",
                UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Map.of("orderId", UUID.randomUUID().toString(), "status", "x"));

        assertThatThrownBy(() -> consumer.consume(event)).isInstanceOf(IllegalArgumentException.class);
    }
}
