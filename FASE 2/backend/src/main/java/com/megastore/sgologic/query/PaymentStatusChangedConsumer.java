package com.megastore.sgologic.query;

import com.megastore.sgologic.messaging.BusinessEventEnvelope;
import com.megastore.sgologic.messaging.InboxService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Proyecta la autorización financiera de PayKlik o crédito propio en el
 * resumen de CUN-09, sin leer tablas privadas de esos módulos.
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
        boolean creditEvent = event.eventType().startsWith("CREDIT_");
        String expectedAggregate = creditEvent ? "CREDIT_OPERATION" : "PaymentAttempt";
        if (!OrderSummaryConsumerConfig.PAYMENT_EVENTS.contains(event.eventType())
                || !expectedAggregate.equals(event.aggregateType())) {
            throw new IllegalArgumentException("Evento incompatible con la proyección de pago");
        }
        Object rawStatus = event.payload().get("status");
        Object rawOrderId = event.payload().get("orderId");
        if (rawStatus == null || rawOrderId == null) {
            throw new IllegalArgumentException("El evento de pago no contiene status u orderId");
        }
        UUID orderId = UUID.fromString(rawOrderId.toString());
        String projectedStatus = creditEvent ? "CREDIT_" + rawStatus : rawStatus.toString();
        inboxService.processOnce(CONSUMER_NAME, event,
                () -> queryService.upsert(orderId, ProjectionSource.PAYMENT, projectedStatus, event.occurredAt()));
    }
}
