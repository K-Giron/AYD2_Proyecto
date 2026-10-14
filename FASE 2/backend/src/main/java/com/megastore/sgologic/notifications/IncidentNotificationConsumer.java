package com.megastore.sgologic.notifications;

import com.megastore.sgologic.messaging.BusinessEventEnvelope;
import com.megastore.sgologic.messaging.InboxService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * CUN-11: convierte el registro de una devolución/incidencia (INC-01) en
 * una notificación no bloqueante. Resuelve el cliente mediante
 * {@link CustomerLookupService} porque Incidents no conoce el
 * {@code customerId} de la orden.
 */
@Component
@ConditionalOnProperty(name = "sgo.messaging.consumers.enabled", havingValue = "true", matchIfMissing = true)
class IncidentNotificationConsumer {

    static final String CONSUMER_NAME = "notifications-incident-registered-v1";

    private final InboxService inboxService;
    private final NotificationService notificationService;
    private final CustomerLookupService customerLookup;

    IncidentNotificationConsumer(InboxService inboxService, NotificationService notificationService,
                                 CustomerLookupService customerLookup) {
        this.inboxService = inboxService;
        this.notificationService = notificationService;
        this.customerLookup = customerLookup;
    }

    @RabbitListener(queues = NotificationConsumerConfig.INCIDENT_QUEUE)
    public void consume(BusinessEventEnvelope event) {
        validateEnvelope(event);
        inboxService.processOnce(CONSUMER_NAME, event, () -> process(event));
    }

    @Transactional
    void process(BusinessEventEnvelope event) {
        Object rawOrderId = event.payload().get("orderId");
        if (rawOrderId == null) return;
        UUID orderId = UUID.fromString(rawOrderId.toString());

        customerLookup.resolveCustomerIdForOrder(orderId).flatMap(customerLookup::resolvePreferredContact)
                .ifPresent(contact -> notificationService.requestNotification(
                        NotificationEventType.INCIDENT_REGISTERED, contact.getChannel(),
                        contact.getDestinationToken(), orderId, event.correlationId()));
    }

    private void validateEnvelope(BusinessEventEnvelope event) {
        if (!NotificationConsumerConfig.INCIDENT_REGISTERED_EVENT.equals(event.eventType())
                || !"INCIDENT".equals(event.aggregateType())) {
            throw new IllegalArgumentException("Evento incompatible con notificaciones de incidencia");
        }
        if (event.payload().get("orderId") == null) {
            throw new IllegalArgumentException("El evento de incidencia no contiene orderId");
        }
    }
}
