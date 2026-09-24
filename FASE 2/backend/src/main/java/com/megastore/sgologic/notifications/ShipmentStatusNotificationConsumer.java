package com.megastore.sgologic.notifications;

import com.megastore.sgologic.messaging.BusinessEventEnvelope;
import com.megastore.sgologic.messaging.InboxService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * CUN-11: convierte los hechos de despacho publicados por Logistics
 * (preparación, tránsito y entrega) en una notificación no bloqueante.
 * Resuelve el cliente mediante {@link CustomerLookupService} porque
 * Logistics no conoce ni debe conocer el {@code customerId} de la orden.
 */
@Component
@ConditionalOnProperty(name = "sgo.messaging.consumers.enabled", havingValue = "true", matchIfMissing = true)
class ShipmentStatusNotificationConsumer {

    static final String CONSUMER_NAME = "notifications-shipment-status-v1";

    private static final Map<String, NotificationEventType> STATUS_TO_EVENT = Map.of(
            "PREPARING", NotificationEventType.SHIPMENT_PREPARING,
            "IN_TRANSIT", NotificationEventType.SHIPMENT_IN_TRANSIT,
            "DELIVERED", NotificationEventType.SHIPMENT_DELIVERED);

    private final InboxService inboxService;
    private final NotificationService notificationService;
    private final CustomerLookupService customerLookup;

    ShipmentStatusNotificationConsumer(InboxService inboxService, NotificationService notificationService,
                                       CustomerLookupService customerLookup) {
        this.inboxService = inboxService;
        this.notificationService = notificationService;
        this.customerLookup = customerLookup;
    }

    @RabbitListener(queues = NotificationConsumerConfig.SHIPMENT_QUEUE)
    public void consume(BusinessEventEnvelope event) {
        validateEnvelope(event);
        inboxService.processOnce(CONSUMER_NAME, event, () -> process(event));
    }

    @Transactional
    void process(BusinessEventEnvelope event) {
        NotificationEventType eventType = STATUS_TO_EVENT.get(String.valueOf(event.payload().get("status")));
        if (eventType == null) return;

        Object rawOrderId = event.payload().get("orderId");
        if (rawOrderId == null) return;
        UUID orderId = UUID.fromString(rawOrderId.toString());

        customerLookup.resolveCustomerIdForOrder(orderId).flatMap(customerLookup::resolvePreferredContact)
                .ifPresent(contact -> notificationService.requestNotification(eventType, contact.getChannel(),
                        contact.getDestinationToken(), orderId, event.correlationId()));
    }

    private void validateEnvelope(BusinessEventEnvelope event) {
        if (!NotificationConsumerConfig.SHIPMENT_STATUS_CHANGED_EVENT.equals(event.eventType())
                || !"SHIPMENT".equals(event.aggregateType())) {
            throw new IllegalArgumentException("Evento incompatible con notificaciones de despacho");
        }
        if (event.payload().get("status") == null) {
            throw new IllegalArgumentException("El evento de despacho no contiene status");
        }
    }
}
