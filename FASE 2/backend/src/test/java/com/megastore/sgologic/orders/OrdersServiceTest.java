package com.megastore.sgologic.orders;

import com.megastore.sgologic.audit.api.AuditRecorder;
import com.megastore.sgologic.credit.api.CreditPort;
import com.megastore.sgologic.fiscal.FiscalProcessingPort;
import com.megastore.sgologic.inventory.InventoryReservationAllocation;
import com.megastore.sgologic.inventory.InventoryReservationPort;
import com.megastore.sgologic.integration.outbox.PublishEvent;
import com.megastore.sgologic.payments.api.PaymentPort;
import com.megastore.sgologic.payments.api.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrdersServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-23T18:00:00Z");
    private static final UUID CUSTOMER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111101");
    private static final UUID CORRELATION_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private final CartRepository cartRepository = mock(CartRepository.class);
    private final CustomerOrderRepository orderRepository = mock(CustomerOrderRepository.class);
    private final OrderStatusHistoryRepository historyRepository = mock(OrderStatusHistoryRepository.class);
    private final InventoryReservationPort inventoryReservationPort = mock(InventoryReservationPort.class);
    private final PaymentPort paymentPort = mock(PaymentPort.class);
    private final CreditPort creditPort = mock(CreditPort.class);
    private final FiscalProcessingPort fiscalProcessingPort = mock(FiscalProcessingPort.class);
    private final PublishEvent eventPublisher = mock(PublishEvent.class);
    private final AuditRecorder auditRecorder = mock(AuditRecorder.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private OrdersService service;

    @BeforeEach
    void setUp() {
        service = new OrdersService(cartRepository, orderRepository, historyRepository,
                inventoryReservationPort, paymentPort, creditPort, fiscalProcessingPort,
                eventPublisher, auditRecorder, clock);
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(CustomerOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(historyRepository.save(any(OrderStatusHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void opensOneActiveCartForAnIdentifiedCustomer() {
        when(cartRepository.findByIdempotencyKey("cart-1")).thenReturn(Optional.empty());
        when(cartRepository.findByCustomerIdAndStatus(CUSTOMER_ID, CartStatus.ACTIVE))
                .thenReturn(Optional.empty());

        Cart cart = service.openOrRecoverCart(CUSTOMER_ID, SalesChannel.WEB, "cart-1", CORRELATION_ID);

        assertThat(cart.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(cart.getOriginChannel()).isEqualTo(SalesChannel.WEB);
        assertThat(cart.getLastChannel()).isEqualTo(SalesChannel.WEB);
        assertThat(cart.getStatus()).isEqualTo(CartStatus.ACTIVE);
        verify(cartRepository).save(cart);
    }

    @Test
    void recoversTheSameCartFromAnotherChannel() {
        Cart existing = Cart.open(CUSTOMER_ID, SalesChannel.WEB, "original", CORRELATION_ID, NOW.minusSeconds(60));
        when(cartRepository.findByIdempotencyKey("mobile-recovery")).thenReturn(Optional.empty());
        when(cartRepository.findByCustomerIdAndStatus(CUSTOMER_ID, CartStatus.ACTIVE))
                .thenReturn(Optional.of(existing));

        Cart recovered = service.openOrRecoverCart(
                CUSTOMER_ID, SalesChannel.MOBILE, "mobile-recovery", CORRELATION_ID);

        assertThat(recovered).isSameAs(existing);
        assertThat(recovered.getOriginChannel()).isEqualTo(SalesChannel.WEB);
        assertThat(recovered.getLastChannel()).isEqualTo(SalesChannel.MOBILE);
        assertThat(recovered.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void repeatedOpenRequestReturnsOriginalCartWithoutMutation() {
        Cart existing = Cart.open(CUSTOMER_ID, SalesChannel.STORE, "same-key", CORRELATION_ID, NOW);
        when(cartRepository.findByIdempotencyKey("same-key")).thenReturn(Optional.of(existing));

        Cart repeated = service.openOrRecoverCart(
                CUSTOMER_ID, SalesChannel.CALL_CENTER, "same-key", CORRELATION_ID);

        assertThat(repeated).isSameAs(existing);
        assertThat(repeated.getLastChannel()).isEqualTo(SalesChannel.STORE);
        verify(cartRepository, never()).save(any());
    }

    @Test
    void addingSameProductUpdatesQuantityInsteadOfDuplicatingLine() {
        Cart cart = activeCartWithItem();
        when(cartRepository.findById(cart.getId())).thenReturn(Optional.of(cart));

        Cart updated = service.putItem(cart.getId(), PRODUCT_ID, "Camisa azul",
                new BigDecimal("349.00"), 3);

        assertThat(updated.getItems()).hasSize(1);
        assertThat(updated.getItems().getFirst().getQuantity()).isEqualTo(3);
        assertThat(updated.total()).isEqualByComparingTo("1047.00");
    }

    @Test
    void checkoutCreatesImmutableOrderSnapshotAndClosesCart() {
        Cart cart = activeCartWithItem();
        when(cartRepository.findById(cart.getId())).thenReturn(Optional.of(cart));
        when(orderRepository.findByCheckoutKey("checkout-1")).thenReturn(Optional.empty());
        when(orderRepository.findByCartId(cart.getId())).thenReturn(Optional.empty());

        CustomerOrder order = service.checkOut(
                cart.getId(), "checkout-1", CORRELATION_ID, "customer:" + CUSTOMER_ID);

        assertThat(cart.getStatus()).isEqualTo(CartStatus.CHECKED_OUT);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.getChannel()).isEqualTo(SalesChannel.WEB);
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getTotal()).isEqualByComparingTo("249.00");
        verify(historyRepository).save(any(OrderStatusHistory.class));
    }

    @Test
    void checkoutIsIdempotentAndDoesNotCreateSecondOrder() {
        Cart cart = activeCartWithItem();
        cart.checkOut(NOW);
        CustomerOrder existing = CustomerOrder.create(cart, "checkout-1", CORRELATION_ID, NOW);
        when(orderRepository.findByCheckoutKey("checkout-1")).thenReturn(Optional.of(existing));

        CustomerOrder repeated = service.checkOut(
                cart.getId(), "checkout-1", CORRELATION_ID, "customer");

        assertThat(repeated).isSameAs(existing);
        verify(orderRepository, never()).save(any());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void emptyCartCannotBecomeOrder() {
        Cart cart = Cart.open(CUSTOMER_ID, SalesChannel.WEB, "empty", CORRELATION_ID, NOW);
        when(cartRepository.findById(cart.getId())).thenReturn(Optional.of(cart));
        when(orderRepository.findByCheckoutKey("checkout-empty")).thenReturn(Optional.empty());
        when(orderRepository.findByCartId(cart.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.checkOut(
                cart.getId(), "checkout-empty", CORRELATION_ID, "customer"))
                .isInstanceOf(EmptyCartException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void stateMachineAllowsDdaSequenceAndRejectsSkippingFiscalApproval() {
        CustomerOrder order = newOrder();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        service.transition(order.getId(), OrderStatus.INVENTORY_RESERVED, "saga", "Reserva creada", CORRELATION_ID);
        service.transition(order.getId(), OrderStatus.PAYMENT_PENDING, "saga", "Intento creado", CORRELATION_ID);
        service.transition(order.getId(), OrderStatus.PAYMENT_APPROVED, "payklik", "Pago aprobado", CORRELATION_ID);

        assertThatThrownBy(() -> service.transition(order.getId(), OrderStatus.CONFIRMED,
                "saga", "Intento de omitir SAT", CORRELATION_ID))
                .isInstanceOf(InvalidOrderTransitionException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_APPROVED);

        ArgumentCaptor<OrderStatusHistory> histories = ArgumentCaptor.forClass(OrderStatusHistory.class);
        verify(historyRepository, times(4)).save(histories.capture());
    }

    @Test
    void reservesEveryOrderLineBeforeAdvancingState() {
        CustomerOrder order = newOrder();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(inventoryReservationPort.reserveOrder(any(), any(), any(), any()))
                .thenReturn(List.<InventoryReservationAllocation>of());

        InventoryReservationOutcome outcome = service.reserveInventory(
                order.getId(), "reserve-order-1", "saga", CORRELATION_ID);

        assertThat(outcome.order().getStatus()).isEqualTo(OrderStatus.INVENTORY_RESERVED);
        verify(inventoryReservationPort).reserveOrder(
                org.mockito.ArgumentMatchers.eq(order.getId()), any(),
                org.mockito.ArgumentMatchers.eq(CORRELATION_ID),
                org.mockito.ArgumentMatchers.eq("reserve-order-1"));
        verify(historyRepository).save(any(OrderStatusHistory.class));
    }

    @Test
    void directPaymentEndpointCannotChargeAnOrderAlreadyApprovedByCredit() {
        CustomerOrder order = newOrder();
        order.transitionTo(OrderStatus.INVENTORY_RESERVED, NOW);
        order.selectPaymentRoute(PaymentRoute.OWN_CREDIT);
        order.transitionTo(OrderStatus.CREDIT_PENDING, NOW);
        order.transitionTo(OrderStatus.CREDIT_APPROVED, NOW);
        order.recordFinancialOperation(UUID.randomUUID());
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.processPayment(order.getId(), PaymentMethod.CARD,
                "GTQ", "tok_should_not_be_used", "late-payment", "customer", CORRELATION_ID))
                .isInstanceOf(InvalidOrderTransitionException.class);

        verify(paymentPort, never()).requestPayment(any());
    }

    private Cart activeCartWithItem() {
        Cart cart = Cart.open(CUSTOMER_ID, SalesChannel.WEB, UUID.randomUUID().toString(), CORRELATION_ID, NOW);
        cart.putItem(PRODUCT_ID, "Camisa manga larga azul", new BigDecimal("249.00"), 1, NOW);
        return cart;
    }

    private CustomerOrder newOrder() {
        Cart cart = activeCartWithItem();
        cart.checkOut(NOW);
        return CustomerOrder.create(cart, UUID.randomUUID().toString(), CORRELATION_ID, NOW);
    }
}
