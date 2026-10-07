package com.megastore.sgologic.query;

import com.megastore.sgologic.messaging.BusinessEventEnvelope;
import com.megastore.sgologic.messaging.InboxService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Proyecta el resultado de pago publicado por Payments (PaymentApproved,
 * PaymentRejected, PaymentIndeterminate; incluida la conciliación con
 * PayKlik) en {@code paymentStatus} del resumen de CUN-09, sin leer tablas
 * de Payments.
 */
@Component
@ConditionalOnProperty(name = "sgo.messaging.consumers.enabled", havingValue = "true", matchIfMissing = true)
class PaymentStatusChangedConsumer {

    static final String CONSUMER_NAME = "query-payment-status-v1";

    private final InboxService inboxService;
    private final OrderSummaryQueryService queryService;

    PaymentStatusChangedConsumer(InboxService inboxService, OrderSummaryQueryService queryService) {
        this.inboxService = inboxService;
        this.queryService = queryService;
    }

    @RabbitListener(queues = OrderSummaryConsumerConfig.PAYMENT_QUEUE)
    public void consume(BusinessEventEnvelope event) {
        if (!OrderSummaryConsumerConfig.PAYMENT_EVENTS.contains(event.eventType())
                || !"PaymentAttempt".equals(event.aggregateType())) {
            throw new IllegalArgumentException("Evento incompatible con la proyección de pago");
        }
        Object rawStatus = event.payload().get("status");
        Object rawOrderId = event.payload().get("orderId");
        if (rawStatus == null || rawOrderId == null) {
            throw new IllegalArgumentException("El evento de pago no contiene status u orderId");
        }
        UUID orderId = UUID.fromString(rawOrderId.toString());
        inboxService.processOnce(CONSUMER_NAME, event,
                () -> queryService.upsert(orderId, ProjectionSource.PAYMENT, rawStatus.toString(), event.occurredAt()));
    }
}
