package com.megastore.sgologic.query;

import com.megastore.sgologic.messaging.BusinessEventEnvelope;
import com.megastore.sgologic.messaging.InboxService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Proyecta eventos de orden con Inbox idempotente para CUN-09. */
@Component
@ConditionalOnProperty(name = "sgo.messaging.consumers.enabled", havingValue = "true", matchIfMissing = true)
class OrderStatusChangedConsumer {

    static final String CONSUMER_NAME = "query-order-summary-v1";

    private final InboxService inboxService;
    private final OrderSummaryQueryService queryService;

    OrderStatusChangedConsumer(InboxService inboxService, OrderSummaryQueryService queryService) {
        this.inboxService = inboxService;
        this.queryService = queryService;
    }

    @RabbitListener(queues = OrderSummaryConsumerConfig.QUEUE)
    public void consume(BusinessEventEnvelope event) {
        if (!"ORDER_STATUS_CHANGED".equals(event.eventType()) || !"ORDER".equals(event.aggregateType())) {
            throw new IllegalArgumentException("Evento incompatible con la proyección de órdenes");
        }
        Object rawStatus = event.payload().get("newStatus");
        if (rawStatus == null || rawStatus.toString().isBlank()) {
            throw new IllegalArgumentException("El evento de orden no contiene newStatus");
        }
        Instant asOf = parseOccurredAt(event);
        inboxService.processOnce(CONSUMER_NAME, event,
                () -> queryService.upsert(event.aggregateId(), ProjectionSource.ORDER,
                        rawStatus.toString(), asOf));
    }

    private Instant parseOccurredAt(BusinessEventEnvelope event) {
        Object payloadTimestamp = event.payload().get("occurredAt");
        return payloadTimestamp == null ? event.occurredAt() : Instant.parse(payloadTimestamp.toString());
    }
}
