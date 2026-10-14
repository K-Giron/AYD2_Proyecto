package com.megastore.sgologic.orders;

import com.megastore.sgologic.fiscal.FiscalProcessingPort;
import com.megastore.sgologic.credit.api.CreditPort;
import com.megastore.sgologic.logistics.api.DispatchPort;
import com.megastore.sgologic.payments.api.PaymentMethod;
import com.megastore.sgologic.payments.api.PaymentPort;
import com.megastore.sgologic.payments.api.PaymentResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PAY-03 en la Saga: si PayKlik no respondió a tiempo (pago indeterminado) y
 * la conciliación ya obtuvo el resultado, repetir la misma solicitud retoma
 * la compra desde ese resultado en vez de quedarse esperando para siempre.
 */
class OrderSagaReconciledPaymentTest {

    private final OrdersService orders = mock(OrdersService.class);
    private final PaymentPort payments = mock(PaymentPort.class);
    private final CreditPort credits = mock(CreditPort.class);
    private final FiscalProcessingPort fiscal = mock(FiscalProcessingPort.class);
    private final DispatchPort dispatch = mock(DispatchPort.class);
    private final OrderSagaService saga = new OrderSagaService(orders, payments, credits, fiscal, dispatch);
    private final UUID orderId = UUID.randomUUID();
    private final RunOrderSagaRequest request = new RunOrderSagaRequest(PaymentRoute.PAYKLIK,
            PaymentMethod.CARD, "GTQ", "tok_payklik_ab12",
            "CF", "Consumidor Final", "Recoger en Tienda Centro (POS-01)");

    private CustomerOrder order(OrderStatus status) {
        CustomerOrder order = mock(CustomerOrder.class);
        when(order.getId()).thenReturn(orderId);
        when(order.getStatus()).thenReturn(status);
        return order;
    }

    private void reconciledAs(String paymentStatus) {
        CustomerOrder indeterminate = order(OrderStatus.PAYMENT_INDETERMINATE);
        when(orders.getOrder(orderId)).thenReturn(indeterminate);
        when(orders.getInventoryReservations(orderId)).thenReturn(List.of());
        when(payments.latestForOrder(orderId)).thenReturn(Optional.of(
                new PaymentResult(UUID.randomUUID(), paymentStatus, "PK-00000002", Instant.now())));
        when(fiscal.latestForOrder(orderId)).thenReturn(Optional.empty());
        when(dispatch.latestForOrder(orderId)).thenReturn(Optional.empty());
    }

    @Test
    void approvedByReconciliationContinuesToTheInvoice() {
        reconciledAs("APPROVED");
        CustomerOrder approved = order(OrderStatus.PAYMENT_APPROVED);
        when(orders.transition(eq(orderId), eq(OrderStatus.PAYMENT_APPROVED), anyString(), anyString(), any()))
                .thenReturn(approved);
        CustomerOrder fiscalPending = order(OrderStatus.FISCAL_PENDING);
        when(orders.requestFiscalDocument(eq(orderId), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new OrderFiscalOutcome(fiscalPending, null));

        OrderSagaResponse response = saga.run(orderId, request, "saga-1", "customer:test", UUID.randomUUID());

        verify(orders).transition(eq(orderId), eq(OrderStatus.PAYMENT_APPROVED), anyString(),
                eq("Pago conciliado con PayKlik (PK-00000002)"), any());
        assertThat(response.sagaStatus()).isEqualTo("WAITING_FISCAL");
    }

    @Test
    void rejectedByReconciliationReleasesTheReservation() {
        reconciledAs("REJECTED");
        CustomerOrder rejected = order(OrderStatus.PAYMENT_REJECTED);
        when(orders.transition(eq(orderId), eq(OrderStatus.PAYMENT_REJECTED), anyString(), anyString(), any()))
                .thenReturn(rejected);
        CustomerOrder cancelled = order(OrderStatus.CANCELLED);
        when(orders.cancelOrder(eq(orderId), anyString(), anyString(), any())).thenReturn(cancelled);

        OrderSagaResponse response = saga.run(orderId, request, "saga-2", "customer:test", UUID.randomUUID());

        assertThat(response.sagaStatus()).isEqualTo("COMPENSATED");
        verify(orders, never()).requestFiscalDocument(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void stillUnknownKeepsWaiting() {
        reconciledAs("INDETERMINATE");

        assertThat(saga.run(orderId, request, "saga-3", "customer:test", UUID.randomUUID()).sagaStatus())
                .isEqualTo("WAITING_PAYMENT");
        verify(orders, never()).transition(any(), any(), anyString(), anyString(), any());
    }
}
