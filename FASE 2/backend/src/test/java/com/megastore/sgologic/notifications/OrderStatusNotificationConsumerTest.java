package com.megastore.sgologic.notifications;

import com.megastore.sgologic.messaging.BusinessEventEnvelope;
import com.megastore.sgologic.messaging.InboxService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderStatusNotificationConsumerTest {

    private final InboxService inbox = mock(InboxService.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final CustomerContactRepository contacts = mock(CustomerContactRepository.class);
    private final OrderStatusNotificationConsumer consumer =
            new OrderStatusNotificationConsumer(inbox, notificationService, contacts);

    @Test
    void createsOneNotificationUsingThePreferredVerifiedContact() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-09-23T23:30:00Z");
        BusinessEventEnvelope event = event(orderId, correlationId, occurredAt,
                Map.of("newStatus", "CONFIRMED", "customerId", customerId.toString()));
        CustomerContact preferred = CustomerContact.verified(customerId, NotificationChannel.EMAIL,
                "cliente@megastore.test", 1, occurredAt);
        when(contacts.findByCustomerIdAndEnabledTrueAndVerifiedAtIsNotNullOrderByPreferenceRankAsc(customerId))
                .thenReturn(List.of(preferred));
        ArgumentCaptor<Runnable> action = ArgumentCaptor.forClass(Runnable.class);

        consumer.consume(event);

        verify(inbox).processOnce(eq(OrderStatusNotificationConsumer.CONSUMER_NAME), eq(event), action.capture());
        action.getValue().run();
        verify(notificationService).requestNotification(NotificationEventType.ORDER_CONFIRMED,
                NotificationChannel.EMAIL, "cliente@megastore.test", orderId, correlationId);
    }

    @Test
    void recordsOtherOrderStatusesWithoutSendingAClientNotification() {
        BusinessEventEnvelope event = event(UUID.randomUUID(), UUID.randomUUID(), Instant.now(),
                Map.of("newStatus", "PAYMENT_APPROVED"));
        ArgumentCaptor<Runnable> action = ArgumentCaptor.forClass(Runnable.class);

        consumer.consume(event);
        verify(inbox).processOnce(eq(OrderStatusNotificationConsumer.CONSUMER_NAME), eq(event), action.capture());
        action.getValue().run();

        verify(notificationService, never()).requestNotification(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsAnEnvelopeWithoutStatus() {
        BusinessEventEnvelope event = event(UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Map.of());

        assertThatThrownBy(() -> consumer.consume(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("newStatus");
    }

    private BusinessEventEnvelope event(UUID orderId, UUID correlationId, Instant occurredAt,
                                        Map<String, Object> payload) {
        return new BusinessEventEnvelope(UUID.randomUUID(), "ORDER_STATUS_CHANGED", "ORDER",
                orderId, correlationId, occurredAt, payload);
    }
}
