package com.megastore.sgologic.notifications;

import com.megastore.sgologic.messaging.BusinessEventEnvelope;
import com.megastore.sgologic.messaging.InboxService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderStatusNotificationConsumerTest {

    private final InboxService inbox = mock(InboxService.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final CustomerLookupService customerLookup = mock(CustomerLookupService.class);
    private final OrderCustomerLinkRepository customerLinkRepository = mock(OrderCustomerLinkRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-23T23:30:00Z"), ZoneOffset.UTC);
    private final OrderStatusNotificationConsumer consumer = new OrderStatusNotificationConsumer(
            inbox, notificationService, customerLookup, customerLinkRepository, clock);

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
        when(customerLookup.resolvePreferredContact(customerId)).thenReturn(Optional.of(preferred));
        ArgumentCaptor<Runnable> action = ArgumentCaptor.forClass(Runnable.class);

        consumer.consume(event);

        verify(inbox).processOnce(eq(OrderStatusNotificationConsumer.CONSUMER_NAME), eq(event), action.capture());
        action.getValue().run();
        verify(notificationService).requestNotification(NotificationEventType.ORDER_CONFIRMED,
                NotificationChannel.EMAIL, "cliente@megastore.test", orderId, correlationId);
        verify(customerLinkRepository).save(any(OrderCustomerLink.class));
    }

    @Test
    void recordsCustomerLinkEvenForStatusesThatDoNotNotify() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        BusinessEventEnvelope event = event(orderId, UUID.randomUUID(), Instant.now(),
                Map.of("newStatus", "PAYMENT_APPROVED", "customerId", customerId.toString()));
        ArgumentCaptor<Runnable> action = ArgumentCaptor.forClass(Runnable.class);

        consumer.consume(event);
        verify(inbox).processOnce(eq(OrderStatusNotificationConsumer.CONSUMER_NAME), eq(event), action.capture());
        action.getValue().run();

        verify(customerLinkRepository).save(any(OrderCustomerLink.class));
        verify(notificationService, never()).requestNotification(any(), any(), any(), any(), any());
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
