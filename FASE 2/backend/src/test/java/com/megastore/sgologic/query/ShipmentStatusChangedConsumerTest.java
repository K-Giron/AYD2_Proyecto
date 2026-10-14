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

class ShipmentStatusChangedConsumerTest {

    @Test
    void projectsPreparingAndInTransitAsDispatchStatus() {
        InboxService inbox = mock(InboxService.class);
        OrderSummaryQueryService query = mock(OrderSummaryQueryService.class);
        ShipmentStatusChangedConsumer consumer = new ShipmentStatusChangedConsumer(inbox, query);
        UUID shipmentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-09-24T05:00:00Z");
        BusinessEventEnvelope event = new BusinessEventEnvelope(UUID.randomUUID(), "SHIPMENT_STATUS_CHANGED",
                "SHIPMENT", shipmentId, UUID.randomUUID(), occurredAt,
                Map.of("orderId", orderId.toString(), "status", "IN_TRANSIT"));
        ArgumentCaptor<Runnable> action = ArgumentCaptor.forClass(Runnable.class);

        consumer.consume(event);

        verify(inbox).processOnce(eq(ShipmentStatusChangedConsumer.CONSUMER_NAME), eq(event), action.capture());
        action.getValue().run();
        verify(query).upsert(orderId, ProjectionSource.DISPATCH, "IN_TRANSIT", occurredAt);
    }

    @Test
    void projectsDeliveredAsDeliveryStatus() {
        InboxService inbox = mock(InboxService.class);
        OrderSummaryQueryService query = mock(OrderSummaryQueryService.class);
        ShipmentStatusChangedConsumer consumer = new ShipmentStatusChangedConsumer(inbox, query);
        UUID orderId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-09-24T05:10:00Z");
        BusinessEventEnvelope event = new BusinessEventEnvelope(UUID.randomUUID(), "SHIPMENT_STATUS_CHANGED",
                "SHIPMENT", UUID.randomUUID(), UUID.randomUUID(), occurredAt,
                Map.of("orderId", orderId.toString(), "status", "DELIVERED"));
        ArgumentCaptor<Runnable> action = ArgumentCaptor.forClass(Runnable.class);

        consumer.consume(event);

        verify(inbox).processOnce(eq(ShipmentStatusChangedConsumer.CONSUMER_NAME), eq(event), action.capture());
        action.getValue().run();
        verify(query).upsert(orderId, ProjectionSource.DELIVERY, "DELIVERED", occurredAt);
    }

    @Test
    void rejectsAnEnvelopeWithoutStatus() {
        ShipmentStatusChangedConsumer consumer = new ShipmentStatusChangedConsumer(
                mock(InboxService.class), mock(OrderSummaryQueryService.class));
        BusinessEventEnvelope event = new BusinessEventEnvelope(UUID.randomUUID(), "SHIPMENT_STATUS_CHANGED",
                "SHIPMENT", UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Map.of());

        assertThatThrownBy(() -> consumer.consume(event)).isInstanceOf(IllegalArgumentException.class);
    }
}
