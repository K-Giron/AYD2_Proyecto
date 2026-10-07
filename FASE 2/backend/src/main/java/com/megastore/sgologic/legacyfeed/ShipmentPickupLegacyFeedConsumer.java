package com.megastore.sgologic.legacyfeed;

import com.megastore.sgologic.integration.PosIntegrationService;
import com.megastore.sgologic.messaging.BusinessEventEnvelope;
import com.megastore.sgologic.messaging.InboxService;
import com.megastore.sgologic.orders.CustomerOrder;
import com.megastore.sgologic.orders.OrdersService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Cuando LogiExpress deja un pedido en la tienda de retiro
 * ({@code SHIPMENT_STATUS_CHANGED} con {@code READY_FOR_PICKUP}), avisa solo
 * al POS de esa tienda ({@code ORDER_READY_FOR_PICKUP}) para que lo entregue
 * al cliente. Mismos reintentos y bandeja de fallidos que los demás eventos POS.
 */
@Component
@ConditionalOnProperty(name = "sgo.messaging.consumers.enabled", havingValue = "true", matchIfMissing = true)
class ShipmentPickupLegacyFeedConsumer {

    static final String CONSUMER_NAME = "legacyfeed-shipment-pickup-v1";

    private final InboxService inboxService;
    private final OrdersService ordersService;
    private final PosOrderPayloads payloads;
    private final PosIntegrationService posService;

    ShipmentPickupLegacyFeedConsumer(InboxService inboxService, OrdersService ordersService,
                                     PosOrderPayloads payloads, PosIntegrationService posService) {
        this.inboxService = inboxService;
        this.ordersService = ordersService;
        this.payloads = payloads;
        this.posService = posService;
    }

    @RabbitListener(queues = LegacyFeedConsumerConfig.SHIPMENT_QUEUE)
    public void consume(BusinessEventEnvelope event) {
        if (!"SHIPMENT_STATUS_CHANGED".equals(event.eventType()) || !"SHIPMENT".equals(event.aggregateType())) {
            throw new IllegalArgumentException("Evento incompatible con el aviso de retiro en tienda");
        }
        inboxService.processOnce(CONSUMER_NAME, event, () -> process(event));
    }

    void process(BusinessEventEnvelope event) {
        if (!"READY_FOR_PICKUP".equals(String.valueOf(event.payload().get("status")))) {
            return;
        }
        Object destination = event.payload().get("destination");
        Object rawOrderId = event.payload().get("orderId");
        if (destination == null || rawOrderId == null) {
            return;
        }
        PosOrderPayloads.pickupStore(destination.toString()).ifPresent(store -> {
            CustomerOrder order = ordersService.getOrder(UUID.fromString(rawOrderId.toString()));
            posService.publish(event.eventId(), "ORDER_READY_FOR_PICKUP", order.getId(),
                    PosOrderPayloads.storeNumber(store), payloads.forOrder(order, "READY_FOR_PICKUP", event),
                    event.correlationId());
        });
    }
}
