package com.megastore.sgologic.notifications;

import com.megastore.sgologic.messaging.BusinessEventEnvelope;
import com.megastore.sgologic.messaging.InboxService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/** Notifica el resultado sin bloquear ni participar en la transacción de crédito. */
@Component
@ConditionalOnProperty(name = "sgo.messaging.consumers.enabled", havingValue = "true", matchIfMissing = true)
class CreditResultNotificationConsumer {

    static final String CONSUMER_NAME = "notifications-credit-result-v1";
    private static final Set<String> ACCEPTED_EVENTS = Set.of("CREDIT_GRANTED", "CREDIT_REJECTED");

    private final InboxService inbox;
    private final NotificationService notifications;
    private final CustomerLookupService customerLookup;

    CreditResultNotificationConsumer(InboxService inbox, NotificationService notifications,
                                     CustomerLookupService customerLookup) {
        this.inbox = inbox;
        this.notifications = notifications;
        this.customerLookup = customerLookup;
    }

    @RabbitListener(queues = NotificationConsumerConfig.CREDIT_QUEUE)
    public void consume(BusinessEventEnvelope event) {
        validate(event);
        inbox.processOnce(CONSUMER_NAME, event, () -> process(event));
    }

    void process(BusinessEventEnvelope event) {
        UUID customerId = UUID.fromString(String.valueOf(event.payload().get("customerId")));
        UUID orderId = UUID.fromString(String.valueOf(event.payload().get("orderId")));
        NotificationEventType eventType = "CREDIT_GRANTED".equals(event.eventType())
                ? NotificationEventType.CREDIT_GRANTED : NotificationEventType.CREDIT_REJECTED;
        customerLookup.resolvePreferredContact(customerId).ifPresent(contact ->
                notifications.requestNotification(eventType, contact.getChannel(),
                        contact.getDestinationToken(), orderId, event.correlationId()));
    }

    private void validate(BusinessEventEnvelope event) {
        if (!"CREDIT_OPERATION".equals(event.aggregateType())
                || !ACCEPTED_EVENTS.contains(event.eventType())
                || event.payload().get("customerId") == null
                || event.payload().get("orderId") == null) {
            throw new IllegalArgumentException("Evento incompatible con notificaciones de crédito");
        }
    }
}
