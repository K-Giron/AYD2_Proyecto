package com.megastore.sgologic.query;

import com.megastore.sgologic.messaging.BusinessEventEnvelope;
import com.megastore.sgologic.messaging.InboxService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Proyecta el registro de una devolución/incidencia (INC-01) en
 * {@code incidentStatus} de CUN-09, sin que Query lea tablas de Incidents.
 */
@Component
@ConditionalOnProperty(name = "sgo.messaging.consumers.enabled", havingValue = "true", matchIfMissing = true)
class IncidentRegisteredConsumer {

    static final String CONSUMER_NAME = "query-incident-registered-v1";

    private final InboxService inboxService;
    private final OrderSummaryQueryService queryService;

    IncidentRegisteredConsumer(InboxService inboxService, OrderSummaryQueryService queryService) {
        this.inboxService = inboxService;
        this.queryService = queryService;
    }

    @RabbitListener(queues = OrderSummaryConsumerConfig.INCIDENT_QUEUE)
    public void consume(BusinessEventEnvelope event) {
        if (!OrderSummaryConsumerConfig.INCIDENT_REGISTERED_EVENT.equals(event.eventType())
                || !"INCIDENT".equals(event.aggregateType())) {
            throw new IllegalArgumentException("Evento incompatible con la proyección de incidencias");
        }
        Object rawOrderId = event.payload().get("orderId");
        Object rawStatus = event.payload().get("status");
        if (rawOrderId == null || rawStatus == null) {
            throw new IllegalArgumentException("El evento de incidencia no contiene orderId o status");
        }
        UUID orderId = UUID.fromString(rawOrderId.toString());
        String status = rawStatus.toString();

        inboxService.processOnce(CONSUMER_NAME, event,
                () -> queryService.upsert(orderId, ProjectionSource.INCIDENT, status, event.occurredAt()));
    }
}
