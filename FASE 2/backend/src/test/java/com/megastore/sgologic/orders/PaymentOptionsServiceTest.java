package com.megastore.sgologic.orders;

import com.megastore.sgologic.credit.api.CreditEvaluationResult;
import com.megastore.sgologic.credit.api.CreditPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentOptionsServiceTest {

    private static final UUID CUSTOMER = UUID.randomUUID();
    private static final UUID CORRELATION = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-10-12T18:00:00Z");
    private final OrdersService orders = mock(OrdersService.class);
    private final CreditPort credit = mock(CreditPort.class);
    private final PaymentOptionsProperties properties = properties();
    private final PaymentOptionsService service = new PaymentOptionsService(
            orders, credit, Clock.fixed(NOW, ZoneOffset.UTC), properties);

    @AfterEach
    void shutdown() {
        service.shutdownExecutor();
    }

    @Test
    void offersCreditOnlyWhenEligibleAndKeepsPayKlikFirst() {
        Cart cart = cart();
        when(orders.getCart(cart.getId())).thenReturn(cart);
        when(credit.evaluate(any())).thenReturn(new CreditEvaluationResult(
                UUID.randomUUID(), true, new BigDecimal("1200.00"),
                "credit-rules-v1", "ELIGIBLE", 30, NOW));

        PaymentOptionsResponse result = service.options(cart.getId(), CUSTOMER, CORRELATION);

        assertThat(result.options()).extracting(PaymentOptionResponse::route)
                .containsExactly(PaymentRoute.PAYKLIK, PaymentRoute.OWN_CREDIT);
        assertThat(result.options().get(1).dueDatePreview()).isEqualTo(LocalDate.of(2026, 11, 11));
    }

    @Test
    void degradesToPayKlikWhenCreditFails() {
        Cart cart = cart();
        when(orders.getCart(cart.getId())).thenReturn(cart);
        when(credit.evaluate(any())).thenThrow(new IllegalStateException("ERP projection unavailable"));

        PaymentOptionsResponse result = service.options(cart.getId(), CUSTOMER, CORRELATION);

        assertThat(result.options()).extracting(PaymentOptionResponse::route)
                .containsExactly(PaymentRoute.PAYKLIK);
    }

    @Test
    void deniesAnotherCustomersCartBeforeEvaluatingCredit() {
        Cart cart = cart();
        when(orders.getCart(cart.getId())).thenReturn(cart);

        assertThatThrownBy(() -> service.options(cart.getId(), UUID.randomUUID(), CORRELATION))
                .isInstanceOf(CartOwnershipException.class);
    }

    @Test
    void returnsPayKlikWithinBoundWhenCreditEvaluationTimesOut() {
        Cart cart = cart();
        when(orders.getCart(cart.getId())).thenReturn(cart);
        when(credit.evaluate(any())).thenAnswer(invocation -> {
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("evaluación cancelada", interrupted);
            }
            throw new AssertionError("La evaluación debió cancelarse.");
        });

        long started = System.nanoTime();
        PaymentOptionsResponse result = service.options(cart.getId(), CUSTOMER, CORRELATION);
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

        assertThat(result.options()).extracting(PaymentOptionResponse::route)
                .containsExactly(PaymentRoute.PAYKLIK);
        assertThat(elapsedMs).isLessThan(500);
    }

    private Cart cart() {
        Cart cart = Cart.open(CUSTOMER, SalesChannel.WEB, "cart", CORRELATION, NOW);
        cart.putItem(UUID.randomUUID(), "Producto", new BigDecimal("800.00"), 1, NOW);
        return cart;
    }

    private static PaymentOptionsProperties properties() {
        PaymentOptionsProperties properties = new PaymentOptionsProperties();
        properties.setCreditTimeoutMs(50);
        properties.setCallPoolSize(1);
        return properties;
    }
}
