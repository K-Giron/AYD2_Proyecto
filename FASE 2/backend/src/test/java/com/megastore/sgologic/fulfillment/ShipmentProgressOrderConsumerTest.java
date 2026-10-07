package com.megastore.sgologic.fulfillment;

import com.megastore.sgologic.messaging.BusinessEventEnvelope;
import com.megastore.sgologic.messaging.InboxService;
import com.megastore.sgologic.orders.CustomerOrder;
import com.megastore.sgologic.orders.OrderStatus;
import com.megastore.sgologic.orders.OrdersService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShipmentProgressOrderConsumerTest {

    private final OrdersService orders = mock(OrdersService.class);
    private final ShipmentProgressOrderConsumer consumer = new ShipmentProgressOrderConsumer(mock(InboxService.class), orders);
    private final UUID orderId = UUID.randomUUID();

    private void orderIn(OrderStatus status) {
        CustomerOrder order = mock(CustomerOrder.class);
        when(order.getStatus()).thenReturn(status);
        when(orders.getOrder(orderId)).thenReturn(order);
    }

    private BusinessEventEnvelope shipment(String status, String source) {
        return new BusinessEventEnvelope(UUID.randomUUID(), "SHIPMENT_STATUS_CHANGED", "SHIPMENT", UUID.randomUUID(),
                UUID.randomUUID(), Instant.now(), Map.of("orderId", orderId.toString(), "status", status, "source", source));
    }

    @Test
    void deliveryWalksTheOrderThroughEveryStep() {
        orderIn(OrderStatus.PREPARING);

        consumer.process(shipment("DELIVERED", "LOGIEXPRESS"));

        InOrder steps = inOrder(orders);
        steps.verify(orders).transition(eq(orderId), eq(OrderStatus.IN_TRANSIT), eq("fulfillment:logiexpress"), anyString(), any());
        steps.verify(orders).transition(eq(orderId), eq(OrderStatus.DELIVERED), eq("fulfillment:logiexpress"), anyString(), any());
    }

    @Test
    void parcelAtThePickupStoreKeepsTheOrderInTransit() {
        orderIn(OrderStatus.PREPARING);

        consumer.process(shipment("READY_FOR_PICKUP", "LOGIEXPRESS"));

        verify(orders).transition(eq(orderId), eq(OrderStatus.IN_TRANSIT), anyString(), anyString(), any());
        verify(orders, never()).transition(eq(orderId), eq(OrderStatus.DELIVERED), anyString(), anyString(), any());
    }

    @Test
    void cancelledOrAlreadyAdvancedOrdersAreLeftAlone() {
        orderIn(OrderStatus.CANCELLED);
        consumer.process(shipment("DELIVERED", "LOGIEXPRESS"));
        orderIn(OrderStatus.DELIVERED);
        consumer.process(shipment("IN_TRANSIT", "LOGIEXPRESS"));

        verify(orders, never()).transition(any(), any(), anyString(), anyString(), any());
    }
}
