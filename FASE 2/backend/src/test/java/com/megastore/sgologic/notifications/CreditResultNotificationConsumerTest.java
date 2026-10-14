package com.megastore.sgologic.notifications;

import com.megastore.sgologic.messaging.BusinessEventEnvelope;
import com.megastore.sgologic.messaging.InboxService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
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

class CreditResultNotificationConsumerTest {

    private final InboxService inbox = mock(InboxService.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final CustomerLookupService customerLookup = mock(CustomerLookupService.class);
    private final CreditResultNotificationConsumer consumer =
            new CreditResultNotificationConsumer(inbox, notifications, customerLookup);

    @Test
    void grantedCreditIsProcessedOnceAndNotifiedToPreferredContact() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        BusinessEventEnvelope event = event("CREDIT_GRANTED", orderId, customerId, correlationId);
        CustomerContact contact = CustomerContact.verified(customerId, NotificationChannel.EMAIL,
                "cliente@megastore.test", 1, Instant.now());
        when(customerLookup.resolvePreferredContact(customerId)).thenReturn(Optional.of(contact));
        ArgumentCaptor<Runnable> action = ArgumentCaptor.forClass(Runnable.class);

        consumer.consume(event);

        verify(inbox).processOnce(eq(CreditResultNotificationConsumer.CONSUMER_NAME), eq(event), action.capture());
        action.getValue().run();
        verify(notifications).requestNotification(NotificationEventType.CREDIT_GRANTED,
                NotificationChannel.EMAIL, "cliente@megastore.test", orderId, correlationId);
    }

    @Test
    void rejectedCreditWithoutContactDoesNotFailOrCreateNotification() {
        UUID customerId = UUID.randomUUID();
        BusinessEventEnvelope event = event("CREDIT_REJECTED", UUID.randomUUID(), customerId, UUID.randomUUID());
        when(customerLookup.resolvePreferredContact(customerId)).thenReturn(Optional.empty());
        ArgumentCaptor<Runnable> action = ArgumentCaptor.forClass(Runnable.class);

        consumer.consume(event);
        verify(inbox).processOnce(eq(CreditResultNotificationConsumer.CONSUMER_NAME), eq(event), action.capture());
        action.getValue().run();

        verify(notifications, never()).requestNotification(any(), any(), any(), any(), any());
    }

    @Test
    void incompatibleEnvelopeIsRejectedBeforeInbox() {
        BusinessEventEnvelope event = new BusinessEventEnvelope(UUID.randomUUID(), "PAYMENT_APPROVED",
                "PAYMENT", UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Map.of());

        assertThatThrownBy(() -> consumer.consume(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("incompatible");
        verify(inbox, never()).processOnce(any(), any(), any());
    }

    private BusinessEventEnvelope event(String type, UUID orderId, UUID customerId, UUID correlationId) {
        return new BusinessEventEnvelope(UUID.randomUUID(), type, "CREDIT_OPERATION", UUID.randomUUID(),
                correlationId, Instant.now(), Map.of(
                "orderId", orderId.toString(),
                "customerId", customerId.toString(),
                "creditOperationId", UUID.randomUUID().toString(),
                "ruleVersion", "credit-rules-v1"));
    }
}
