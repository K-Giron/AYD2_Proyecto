package com.megastore.sgologic.fulfillment;

import com.megastore.sgologic.messaging.BusinessEventEnvelope;
import com.megastore.sgologic.messaging.InboxService;
import com.megastore.sgologic.orders.CustomerOrder;
import com.megastore.sgologic.orders.OrderStatus;
import com.megastore.sgologic.orders.OrdersService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Consume {@code SHIPMENT_STATUS_CHANGED} y avanza la orden:
 * tránsito o llegada a la tienda de retiro → {@code IN_TRANSIT}; entrega →
 * {@code DELIVERED}. Recorre los pasos intermedios en orden (la máquina de
 * estados de la orden no se salta pasos) y nunca retrocede. Idempotente por
 * el Inbox y porque una orden ya avanzada no se toca.
 */
@Component
@ConditionalOnProperty(name = "sgo.messaging.consumers.enabled", havingValue = "true", matchIfMissing = true)
class ShipmentProgressOrderConsumer {

    static final String CONSUMER_NAME = "fulfillment-shipment-status-v1";
    private static final List<OrderStatus> PATH = List.of(
            OrderStatus.CONFIRMED, OrderStatus.PREPARING, OrderStatus.IN_TRANSIT, OrderStatus.DELIVERED);

    private final InboxService inboxService;
    private final OrdersService ordersService;

    ShipmentProgressOrderConsumer(InboxService inboxService, OrdersService ordersService) {
        this.inboxService = inboxService;
        this.ordersService = ordersService;
    }

    @RabbitListener(queues = FulfillmentConsumerConfig.QUEUE)
    public void consume(BusinessEventEnvelope event) {
        if (!"SHIPMENT_STATUS_CHANGED".equals(event.eventType()) || !"SHIPMENT".equals(event.aggregateType())) {
            throw new IllegalArgumentException("Evento incompatible con el cumplimiento de órdenes");
        }
        inboxService.processOnce(CONSUMER_NAME, event, () -> process(event));
    }

    void process(BusinessEventEnvelope event) {
        String status = String.valueOf(event.payload().get("status"));
        OrderStatus target = switch (status) {
            case "IN_TRANSIT", "READY_FOR_PICKUP" -> OrderStatus.IN_TRANSIT;
            case "DELIVERED" -> OrderStatus.DELIVERED;
            default -> null;
        };
        Object rawOrderId = event.payload().get("orderId");
        if (target == null || rawOrderId == null) {
            return;
        }
        String source = String.valueOf(event.payload().getOrDefault("source", "SGO-LOGIC"));
        UUID orderId = UUID.fromString(rawOrderId.toString());
        CustomerOrder order = ordersService.getOrder(orderId);
        int current = PATH.indexOf(order.getStatus());
        int goal = PATH.indexOf(target);
        // Fuera del camino de despacho (cancelada, sin confirmar) o ya avanzada: nada que hacer.
        for (int step = current + 1; current >= 0 && step <= goal; step++) {
            OrderStatus next = PATH.get(step);
            ordersService.transition(orderId, next, "fulfillment:" + source.toLowerCase(java.util.Locale.ROOT),
                    reason(next, status, source), event.correlationId());
        }
    }

    private static String reason(OrderStatus next, String shipmentStatus, String source) {
        String by = "LOGIEXPRESS".equals(source) ? "LogiExpress" : source;
        return switch (next) {
            case PREPARING -> "Despacho en preparación (" + by + ")";
            case IN_TRANSIT -> "READY_FOR_PICKUP".equals(shipmentStatus)
                    ? by + " dejó el pedido en la tienda de retiro"
                    : by + " recolectó el pedido y va en camino";
            case DELIVERED -> "Entrega confirmada por " + by;
            default -> "Avance del despacho (" + by + ")";
        };
    }
}
