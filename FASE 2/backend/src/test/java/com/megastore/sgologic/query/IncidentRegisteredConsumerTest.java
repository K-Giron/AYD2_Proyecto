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

class IncidentRegisteredConsumerTest {

    @Test
    void projectsIncidentStatus() {
        InboxService inbox = mock(InboxService.class);
        OrderSummaryQueryService query = mock(OrderSummaryQueryService.class);
        IncidentRegisteredConsumer consumer = new IncidentRegisteredConsumer(inbox, query);
        UUID incidentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-09-24T05:20:00Z");
        BusinessEventEnvelope event = new BusinessEventEnvelope(UUID.randomUUID(), "INCIDENT_REGISTERED",
                "INCIDENT", incidentId, UUID.randomUUID(), occurredAt,
                Map.of("orderId", orderId.toString(), "status", "REGISTERED", "type", "RETURN"));
        ArgumentCaptor<Runnable> action = ArgumentCaptor.forClass(Runnable.class);

        consumer.consume(event);

        verify(inbox).processOnce(eq(IncidentRegisteredConsumer.CONSUMER_NAME), eq(event), action.capture());
        action.getValue().run();
        verify(query).upsert(orderId, ProjectionSource.INCIDENT, "REGISTERED", occurredAt);
    }

    @Test
    void rejectsAnEnvelopeWithoutOrderId() {
        IncidentRegisteredConsumer consumer = new IncidentRegisteredConsumer(
                mock(InboxService.class), mock(OrderSummaryQueryService.class));
        BusinessEventEnvelope event = new BusinessEventEnvelope(UUID.randomUUID(), "INCIDENT_REGISTERED",
                "INCIDENT", UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Map.of("status", "REGISTERED"));

        assertThatThrownBy(() -> consumer.consume(event)).isInstanceOf(IllegalArgumentException.class);
    }
}
