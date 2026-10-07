package com.megastore.sgologic.inventorysync;

import com.megastore.sgologic.messaging.BusinessEventEnvelope;
import com.megastore.sgologic.messaging.InboxService;
import com.megastore.sgologic.orders.CustomerOrder;
import com.megastore.sgologic.orders.OrderLine;
import com.megastore.sgologic.orders.OrdersService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderConfirmedInventorySyncConsumerTest {

    private final InboxService inbox = mock(InboxService.class);
    private final OrdersService ordersService = mock(OrdersService.class);
    private final InventorySyncService inventorySyncService = mock(InventorySyncService.class);
    private final OrderOriginRepository orderOriginRepository = mock(OrderOriginRepository.class);
    private final OrderConfirmedInventorySyncConsumer consumer = new OrderConfirmedInventorySyncConsumer(
            inbox, ordersService, inventorySyncService, orderOriginRepository);

    @Test
    void confirmedOrderTriggersSyncWithEachLine() {
        UUID orderId = UUID.randomUUID();
        OrderLine shirt = mock(OrderLine.class);
        when(shirt.getProductId()).thenReturn(UUID.randomUUID());
        when(shirt.getProductName()).thenReturn("Camisa manga larga azul");
        when(shirt.getQuantity()).thenReturn(2);
        CustomerOrder order = mock(CustomerOrder.class);
        when(order.getId()).thenReturn(orderId);
        when(order.getItems()).thenReturn(List.of(shirt));
        when(ordersService.getOrder(orderId)).thenReturn(order);
        BusinessEventEnvelope event = event(orderId, Map.of("newStatus", "CONFIRMED"));
        ArgumentCaptor<Runnable> action = ArgumentCaptor.forClass(Runnable.class);

        consumer.consume(event);

        verify(inbox).processOnce(eq(OrderConfirmedInventorySyncConsumer.CONSUMER_NAME), eq(event), action.capture());
        action.getValue().run();

        ArgumentCaptor<List<SoldLine>> linesCaptor = ArgumentCaptor.forClass(List.class);
        verify(inventorySyncService).triggerSync(eq(orderId), linesCaptor.capture(), eq(null));
        assertThat(linesCaptor.getValue()).containsExactly(
                new SoldLine(shirt.getProductId(), "Camisa manga larga azul", 2));
    }

    @Test
    void confirmedStoreOrderIsRoutedToItsPointOfSale() {
        UUID orderId = UUID.randomUUID();
        CustomerOrder order = mock(CustomerOrder.class);
        when(order.getId()).thenReturn(orderId);
        when(order.getItems()).thenReturn(List.of());
        when(ordersService.getOrder(orderId)).thenReturn(order);
        when(orderOriginRepository.findById(orderId))
                .thenReturn(Optional.of(OrderOrigin.of(orderId, "POS-01", Instant.now())));
        BusinessEventEnvelope event = event(orderId, Map.of("newStatus", "CONFIRMED"));
        ArgumentCaptor<Runnable> action = ArgumentCaptor.forClass(Runnable.class);

        consumer.consume(event);
        verify(inbox).processOnce(eq(OrderConfirmedInventorySyncConsumer.CONSUMER_NAME), eq(event), action.capture());
        action.getValue().run();

        verify(inventorySyncService).triggerSync(eq(orderId), any(), eq("POS-01"));
    }

    @Test
    void ignoresStatusesOtherThanConfirmed() {
        UUID orderId = UUID.randomUUID();
        BusinessEventEnvelope event = event(orderId, Map.of("newStatus", "PAYMENT_APPROVED"));
        ArgumentCaptor<Runnable> action = ArgumentCaptor.forClass(Runnable.class);

        consumer.consume(event);

        verify(inbox).processOnce(eq(OrderConfirmedInventorySyncConsumer.CONSUMER_NAME), eq(event), action.capture());
        action.getValue().run();

        verify(ordersService, never()).getOrder(any());
        verify(inventorySyncService, never()).triggerSync(any(), any(), any());
    }

    @Test
    void rejectsAnEnvelopeOfADifferentEventType() {
        BusinessEventEnvelope event = new BusinessEventEnvelope(UUID.randomUUID(), "SHIPMENT_STATUS_CHANGED",
                "SHIPMENT", UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Map.of());

        assertThatThrownBy(() -> consumer.consume(event)).isInstanceOf(IllegalArgumentException.class);
    }

    private BusinessEventEnvelope event(UUID orderId, Map<String, Object> payload) {
        return new BusinessEventEnvelope(UUID.randomUUID(), "ORDER_STATUS_CHANGED", "ORDER",
                orderId, UUID.randomUUID(), Instant.now(), payload);
    }
}
