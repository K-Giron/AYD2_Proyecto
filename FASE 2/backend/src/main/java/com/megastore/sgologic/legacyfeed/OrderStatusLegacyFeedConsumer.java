package com.megastore.sgologic.legacyfeed;

import com.megastore.sgologic.integration.ErpIntegrationService;
import com.megastore.sgologic.integration.PosIntegrationService;
import com.megastore.sgologic.messaging.BusinessEventEnvelope;
import com.megastore.sgologic.messaging.InboxService;
import com.megastore.sgologic.orders.CustomerOrder;
import com.megastore.sgologic.orders.OrdersService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Consume {@code ORDER_STATUS_CHANGED} del Outbox y, sin intervención manual:
 * <ul>
 *   <li>al confirmarse la orden la registra para el lote ERP (flujo feliz,
 *   paso 9: "consolida las ventas confirmadas");</li>
 *   <li>emite el cambio a los POS de las 32 tiendas (paso 10: "mantener la
 *   coherencia de información en las 32 tiendas físicas"), incluyendo, si es
 *   una compra para recoger en tienda, cuál tienda la entrega.</li>
 * </ul>
 * Idempotente: el Inbox descarta el evento repetido y las entregas POS se
 * deduplican por (evento, tienda).
 */
@Component
@ConditionalOnProperty(name = "sgo.messaging.consumers.enabled", havingValue = "true", matchIfMissing = true)
class OrderStatusLegacyFeedConsumer {

    static final String CONSUMER_NAME = "legacyfeed-order-status-v1";
    static final int STORE_COUNT = 32;
    /** Estados que cambian algo para una tienda: venta confirmada, preparación, tránsito, entrega o cancelación. */
    static final Set<String> POS_STATUSES = Set.of("CONFIRMED", "PREPARING", "IN_TRANSIT", "DELIVERED", "CANCELLED");

    private final InboxService inboxService;
    private final OrdersService ordersService;
    private final PosOrderPayloads payloads;
    private final ErpIntegrationService erpService;
    private final PosIntegrationService posService;

    OrderStatusLegacyFeedConsumer(InboxService inboxService, OrdersService ordersService,
                                  PosOrderPayloads payloads, ErpIntegrationService erpService,
                                  PosIntegrationService posService) {
        this.inboxService = inboxService;
        this.ordersService = ordersService;
        this.payloads = payloads;
        this.erpService = erpService;
        this.posService = posService;
    }

    @RabbitListener(queues = LegacyFeedConsumerConfig.QUEUE)
    public void consume(BusinessEventEnvelope event) {
        if (!"ORDER_STATUS_CHANGED".equals(event.eventType()) || !"ORDER".equals(event.aggregateType())) {
            throw new IllegalArgumentException("Evento incompatible con la alimentación de ERP/POS");
        }
        inboxService.processOnce(CONSUMER_NAME, event, () -> process(event));
    }

    @Transactional
    void process(BusinessEventEnvelope event) {
        Object rawStatus = event.payload().get("newStatus");
        String newStatus = rawStatus == null ? "" : rawStatus.toString();
        if (!POS_STATUSES.contains(newStatus)) {
            return;
        }
        CustomerOrder order = ordersService.getOrder(event.aggregateId());
        if ("CONFIRMED".equals(newStatus)) {
            BigDecimal[] split = ErpIntegrationService.splitIva(order.getTotal());
            erpService.registerConfirmedSale(order.getId(), split[0], split[1], order.getTotal(), "GTQ",
                    event.occurredAt() == null ? order.getUpdatedAt() : event.occurredAt(),
                    order.getChannel().name(), "POS-ONLINE", order.getId().toString());
        }
        String payload = payloads.forOrder(order, newStatus, event);
        for (int store = 1; store <= STORE_COUNT; store++) {
            posService.publish(event.eventId(), "ORDER_" + newStatus, order.getId(), store, payload,
                    event.correlationId());
        }
    }
}
