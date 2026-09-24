package com.megastore.sgologic.query;

import com.megastore.sgologic.messaging.BusinessEventEnvelope;
import com.megastore.sgologic.messaging.InboxService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Proyecta los hechos de despacho publicados por Logistics (LOG-01/02) en
 * CUN-09: {@code PREPARING}/{@code IN_TRANSIT} alimentan {@code dispatchStatus}
 * y {@code DELIVERED} alimenta {@code deliveryStatus}, sin que Query lea
 * tablas de Logistics.
 */
@Component
@ConditionalOnProperty(name = "sgo.messaging.consumers.enabled", havingValue = "true", matchIfMissing = true)
class ShipmentStatusChangedConsumer {

    static final String CONSUMER_NAME = "query-shipment-status-v1";

    private final InboxService inboxService;
    private final OrderSummaryQueryService queryService;

    ShipmentStatusChangedConsumer(InboxService inboxService, OrderSummaryQueryService queryService) {
        this.inboxService = inboxService;
        this.queryService = queryService;
    }

    @RabbitListener(queues = OrderSummaryConsumerConfig.SHIPMENT_QUEUE)
    public void consume(BusinessEventEnvelope event) {
        if (!OrderSummaryConsumerConfig.SHIPMENT_STATUS_CHANGED_EVENT.equals(event.eventType())
                || !"SHIPMENT".equals(event.aggregateType())) {
            throw new IllegalArgumentException("Evento incompatible con la proyección de despacho");
        }
        Object rawStatus = event.payload().get("status");
        Object rawOrderId = event.payload().get("orderId");
        if (rawStatus == null || rawOrderId == null) {
            throw new IllegalArgumentException("El evento de despacho no contiene status u orderId");
        }
        String status = rawStatus.toString();
        UUID orderId = UUID.fromString(rawOrderId.toString());
        ProjectionSource source = "DELIVERED".equals(status) ? ProjectionSource.DELIVERY : ProjectionSource.DISPATCH;

        inboxService.processOnce(CONSUMER_NAME, event,
                () -> queryService.upsert(orderId, source, status, event.occurredAt()));
    }
}
