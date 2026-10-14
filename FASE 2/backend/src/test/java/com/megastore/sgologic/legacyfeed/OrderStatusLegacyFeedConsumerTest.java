package com.megastore.sgologic.legacyfeed;

import com.megastore.sgologic.integration.ErpIntegrationService;
import com.megastore.sgologic.integration.PosIntegrationService;
import com.megastore.sgologic.logistics.Shipment;
import com.megastore.sgologic.logistics.ShipmentService;
import com.megastore.sgologic.messaging.BusinessEventEnvelope;
import com.megastore.sgologic.messaging.InboxService;
import com.megastore.sgologic.orders.CustomerOrder;
import com.megastore.sgologic.orders.OrdersService;
import com.megastore.sgologic.orders.PaymentRoute;
import com.megastore.sgologic.orders.SalesChannel;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderStatusLegacyFeedConsumerTest {

    private final OrdersService orders = mock(OrdersService.class);
    private final ShipmentService shipments = mock(ShipmentService.class);
    private final ErpIntegrationService erp = mock(ErpIntegrationService.class);
    private final PosIntegrationService pos = mock(PosIntegrationService.class);
    private final OrderStatusLegacyFeedConsumer consumer = new OrderStatusLegacyFeedConsumer(
            mock(InboxService.class), orders, new PosOrderPayloads(shipments, JsonMapper.builder().build()), erp, pos);
    private final UUID orderId = UUID.randomUUID();

    private BusinessEventEnvelope event(String status) {
        return new BusinessEventEnvelope(UUID.randomUUID(), "ORDER_STATUS_CHANGED", "ORDER", orderId, UUID.randomUUID(),
                Instant.parse("2026-10-06T20:00:00Z"), Map.of("newStatus", status));
    }

    private void orderTotal(String total) {
        orderTotal(total, PaymentRoute.PAYKLIK);
    }

    private void orderTotal(String total, PaymentRoute paymentRoute) {
        CustomerOrder order = mock(CustomerOrder.class);
        when(order.getId()).thenReturn(orderId);
        when(order.getTotal()).thenReturn(new BigDecimal(total));
        when(order.getChannel()).thenReturn(SalesChannel.WEB);
        when(order.getPaymentRoute()).thenReturn(paymentRoute);
        when(order.getItems()).thenReturn(List.of());
        when(order.getUpdatedAt()).thenReturn(Instant.now());
        when(orders.getOrder(orderId)).thenReturn(order);
    }

    @Test
    void confirmedOrderGoesToTheErpAndToThe32Stores() {
        orderTotal("1120.00");
        when(shipments.getShipmentsForOrder(orderId)).thenReturn(List.of());

        consumer.process(event("CONFIRMED"));

        verify(erp).registerConfirmedSale(eq(orderId), eq(new BigDecimal("1000.00")), eq(new BigDecimal("120.00")),
                eq(new BigDecimal("1120.00")), eq("GTQ"), any(), eq("WEB"), eq("POS-ONLINE"),
                eq(orderId.toString()), eq("PAYKLIK"));
        verify(pos, times(32)).publish(any(), eq("ORDER_CONFIRMED"), eq(orderId), anyInt(), anyString(), any());
    }

    @Test
    void confirmedCreditSaleKeepsItsFinancialRouteInTheErp() {
        orderTotal("399.00", PaymentRoute.OWN_CREDIT);
        when(shipments.getShipmentsForOrder(orderId)).thenReturn(List.of());

        consumer.process(event("CONFIRMED"));

        verify(erp).registerConfirmedSale(eq(orderId), eq(new BigDecimal("356.25")), eq(new BigDecimal("42.75")),
                eq(new BigDecimal("399.00")), eq("GTQ"), any(), eq("WEB"), eq("POS-ONLINE"),
                eq(orderId.toString()), eq("OWN_CREDIT"));
    }

    @Test
    void pickupAtAStoreTellsTheStoresWhichOneDeliversIt() {
        orderTotal("749.00");
        Shipment shipment = mock(Shipment.class);
        when(shipment.getDestination()).thenReturn("Recoger en Tienda Centro (tienda física 1) (POS-01) — 6a. Avenida");
        when(shipment.getOriginCode()).thenReturn("CD-WEB");
        when(shipments.getShipmentsForOrder(orderId)).thenReturn(List.of(shipment));

        consumer.process(event("PREPARING"));

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(pos, times(32)).publish(any(), eq("ORDER_PREPARING"), eq(orderId), anyInt(), payload.capture(), any());
        assertThat(payload.getValue()).contains("\"pickupStore\":\"POS-01\"", "\"origin\":\"CD-WEB\"", "\"status\":\"PREPARING\"");
        verify(erp, never()).registerConfirmedSale(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void intermediateStatusesAreNotSentToLegacySystems() {
        consumer.process(event("FISCAL_PENDING"));

        verify(orders, never()).getOrder(any());
        verify(pos, never()).publish(any(), any(), any(), anyInt(), any(), any());
    }
}
