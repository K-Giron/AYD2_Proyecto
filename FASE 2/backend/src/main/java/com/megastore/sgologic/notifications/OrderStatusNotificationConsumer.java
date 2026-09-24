package com.megastore.sgologic.notifications;

import com.megastore.sgologic.messaging.BusinessEventEnvelope;
import com.megastore.sgologic.messaging.InboxService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/** Convierte una confirmación de orden en una notificación no bloqueante. */
@Component
@ConditionalOnProperty(name = "sgo.messaging.consumers.enabled", havingValue = "true", matchIfMissing = true)
class OrderStatusNotificationConsumer {

    static final String CONSUMER_NAME = "notifications-order-status-v1";

    private final InboxService inboxService;
    private final NotificationService notificationService;
    private final CustomerContactRepository contactRepository;

    OrderStatusNotificationConsumer(InboxService inboxService,
                                    NotificationService notificationService,
                                    CustomerContactRepository contactRepository) {
        this.inboxService = inboxService;
        this.notificationService = notificationService;
        this.contactRepository = contactRepository;
    }

    @RabbitListener(queues = NotificationConsumerConfig.QUEUE)
    public void consume(BusinessEventEnvelope event) {
        validateEnvelope(event);
        inboxService.processOnce(CONSUMER_NAME, event, () -> process(event));
    }

    private void process(BusinessEventEnvelope event) {
        if (!"CONFIRMED".equals(event.payload().get("newStatus").toString())) return;

        Object rawCustomerId = event.payload().get("customerId");
        if (rawCustomerId == null) return;

        List<CustomerContact> contacts = contactRepository
                .findByCustomerIdAndEnabledTrueAndVerifiedAtIsNotNullOrderByPreferenceRankAsc(
                        UUID.fromString(rawCustomerId.toString()));
        if (contacts.isEmpty()) return;

        CustomerContact preferred = contacts.getFirst();
        notificationService.requestNotification(NotificationEventType.ORDER_CONFIRMED,
                preferred.getChannel(), preferred.getDestinationToken(), event.aggregateId(),
                event.correlationId());
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
