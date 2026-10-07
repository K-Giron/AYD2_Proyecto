package com.megastore.sgologic.inventorysync;

import com.megastore.sgologic.messaging.BusinessEventEnvelope;
import com.megastore.sgologic.messaging.InboxService;
import com.megastore.sgologic.orders.CustomerOrder;
import com.megastore.sgologic.orders.OrdersService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reacciona a la confirmación de una orden (FISCAL_APPROVED → CONFIRMED,
 * el mismo filtro que ya usa {@code notifications.OrderStatusNotificationConsumer})
 * y dispara la sincronización hacia inventory-service para cada línea
 * vendida: si la terminal de una tienda física registró el origen de la
 * orden, la venta descuenta la existencia de esa tienda; si no, es una
 * venta web y sale del almacén central hacia "Tienda Online". El evento
 * no trae las líneas — se vuelve a leer la orden completa vía
 * {@code OrdersService.getOrder}, en el mismo proceso, sin HTTP.
 */
@Component
@ConditionalOnProperty(name = "sgo.messaging.consumers.enabled", havingValue = "true", matchIfMissing = true)
class OrderConfirmedInventorySyncConsumer {

    static final String CONSUMER_NAME = "inventorysync-order-status-v1";

    private final InboxService inboxService;
    private final OrdersService ordersService;
    private final InventorySyncService inventorySyncService;
    private final OrderOriginRepository orderOriginRepository;

    OrderConfirmedInventorySyncConsumer(InboxService inboxService, OrdersService ordersService,
                                         InventorySyncService inventorySyncService,
                                         OrderOriginRepository orderOriginRepository) {
        this.inboxService = inboxService;
        this.ordersService = ordersService;
        this.inventorySyncService = inventorySyncService;
        this.orderOriginRepository = orderOriginRepository;
    }

    @RabbitListener(queues = InventorySyncConsumerConfig.QUEUE)
    public void consume(BusinessEventEnvelope event) {
        validateEnvelope(event);
        inboxService.processOnce(CONSUMER_NAME, event, () -> process(event));
    }

    @Transactional
    void process(BusinessEventEnvelope event) {
        Object newStatus = event.payload().get("newStatus");
        if (newStatus == null || !"CONFIRMED".equals(newStatus.toString())) {
            return;
        }
        CustomerOrder order = ordersService.getOrder(event.aggregateId());
        var lines = order.getItems().stream()
                .map(item -> new SoldLine(item.getProductId(), item.getProductName(), item.getQuantity()))
                .toList();
        String storePointOfSaleCode = orderOriginRepository.findById(order.getId())
                .map(OrderOrigin::getPointOfSaleCode)
                .orElse(null);
        inventorySyncService.triggerSync(order.getId(), lines, storePointOfSaleCode);
    }

    private void validateEnvelope(BusinessEventEnvelope event) {
        if (!"ORDER_STATUS_CHANGED".equals(event.eventType()) || !"ORDER".equals(event.aggregateType())) {
            throw new IllegalArgumentException("Evento incompatible con sincronización de inventario");
        }
    }
}
