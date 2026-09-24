package com.megastore.sgologic.notifications;

import com.megastore.sgologic.messaging.BusinessEventEnvelope;
import com.megastore.sgologic.messaging.InboxService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * Convierte una confirmación de orden en una notificación no bloqueante y
 * recuerda {@code orderId -> customerId} para que los eventos de despacho
 * (Logistics) e incidencia (Incidents), que no conocen al cliente, también
 * puedan resolver el contacto.
 */
@Component
@ConditionalOnProperty(name = "sgo.messaging.consumers.enabled", havingValue = "true", matchIfMissing = true)
class OrderStatusNotificationConsumer {

    static final String CONSUMER_NAME = "notifications-order-status-v1";

    private final InboxService inboxService;
    private final NotificationService notificationService;
    private final CustomerLookupService customerLookup;
    private final OrderCustomerLinkRepository customerLinkRepository;
    private final Clock clock;

    OrderStatusNotificationConsumer(InboxService inboxService,
                                    NotificationService notificationService,
                                    CustomerLookupService customerLookup,
                                    OrderCustomerLinkRepository customerLinkRepository,
                                    Clock clock) {
        this.inboxService = inboxService;
        this.notificationService = notificationService;
        this.customerLookup = customerLookup;
        this.customerLinkRepository = customerLinkRepository;
        this.clock = clock;
    }

    @RabbitListener(queues = NotificationConsumerConfig.QUEUE)
    public void consume(BusinessEventEnvelope event) {
        validateEnvelope(event);
        inboxService.processOnce(CONSUMER_NAME, event, () -> process(event));
    }

    @Transactional
    void process(BusinessEventEnvelope event) {
        Object rawCustomerId = event.payload().get("customerId");
        UUID customerId = rawCustomerId == null ? null : UUID.fromString(rawCustomerId.toString());
        if (customerId != null) {
            customerLinkRepository.save(OrderCustomerLink.of(event.aggregateId(), customerId, clock.instant()));
        }

        if (!"CONFIRMED".equals(event.payload().get("newStatus").toString()) || customerId == null) return;
        customerLookup.resolvePreferredContact(customerId).ifPresent(contact ->
                notificationService.requestNotification(NotificationEventType.ORDER_CONFIRMED,
                        contact.getChannel(), contact.getDestinationToken(), event.aggregateId(),
                        event.correlationId()));
    }

    private void validateEnvelope(BusinessEventEnvelope event) {
        if (!"ORDER_STATUS_CHANGED".equals(event.eventType()) || !"ORDER".equals(event.aggregateType())) {
            throw new IllegalArgumentException("Evento incompatible con notificaciones de orden");
        }
        Object status = event.payload().get("newStatus");
        if (status == null || status.toString().isBlank()) {
            throw new IllegalArgumentException("El evento de orden no contiene newStatus");
        }
    }
}
