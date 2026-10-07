package com.megastore.sgologic.legacyfeed;

import com.megastore.sgologic.integration.PosIntegrationService;
import com.megastore.sgologic.logistics.ShipmentService;
import com.megastore.sgologic.messaging.BusinessEventEnvelope;
import com.megastore.sgologic.messaging.InboxService;
import com.megastore.sgologic.orders.CustomerOrder;
import com.megastore.sgologic.orders.OrdersService;
import com.megastore.sgologic.orders.SalesChannel;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShipmentPickupLegacyFeedConsumerTest {

    private final OrdersService orders = mock(OrdersService.class);
    private final ShipmentService shipments = mock(ShipmentService.class);
    private final PosIntegrationService pos = mock(PosIntegrationService.class);
    private final ShipmentPickupLegacyFeedConsumer consumer = new ShipmentPickupLegacyFeedConsumer(mock(InboxService.class),
            orders, new PosOrderPayloads(shipments, JsonMapper.builder().build()), pos);
    private final UUID orderId = UUID.randomUUID();

    private BusinessEventEnvelope shipment(String status, String destination) {
        return new BusinessEventEnvelope(UUID.randomUUID(), "SHIPMENT_STATUS_CHANGED", "SHIPMENT", UUID.randomUUID(),
                UUID.randomUUID(), Instant.now(), Map.of("orderId", orderId.toString(), "status", status, "destination", destination));
    }

    @Test
    void onlyThePickupStoreIsToldThatTheParcelArrived() {
        CustomerOrder order = mock(CustomerOrder.class);
        when(order.getId()).thenReturn(orderId);
        when(order.getTotal()).thenReturn(new BigDecimal("749.00"));
        when(order.getChannel()).thenReturn(SalesChannel.WEB);
        when(order.getItems()).thenReturn(List.of());
        when(orders.getOrder(orderId)).thenReturn(order);
        when(shipments.getShipmentsForOrder(orderId)).thenReturn(List.of());

        consumer.process(shipment("READY_FOR_PICKUP", "Recoger en Tienda Centro (POS-01) — 6a. Avenida"));

        verify(pos).publish(any(), eq("ORDER_READY_FOR_PICKUP"), eq(orderId), eq(1), anyString(), any());
    }

    @Test
    void homeDeliveriesAndOtherStatusesAreIgnored() {
        consumer.process(shipment("READY_FOR_PICKUP", "Zona 10, Ciudad de Guatemala"));
        consumer.process(shipment("IN_TRANSIT", "Recoger en Tienda Centro (POS-01)"));

        verify(pos, never()).publish(any(), any(), any(), anyInt(), any(), any());
    }
}
