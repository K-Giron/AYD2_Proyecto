package com.megastore.sgologic.query;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * QRY-01 / CDU-09: consolidación de varias áreas en un solo registro y
 * marca de "posiblemente desactualizado" (E1) sin fallar toda la consulta.
 */
class OrderSummaryQueryServiceTest {

    private final OrderSummaryProjectionRepository repository = mock(OrderSummaryProjectionRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-18T12:00:00Z"), ZoneOffset.UTC);
    private final OrderSummaryQueryService service = new OrderSummaryQueryService(repository, clock);

    @Test
    void upsertMergesMultipleSourcesIntoOneProjection() {
        UUID orderId = UUID.randomUUID();
        OrderSummaryProjection row = OrderSummaryProjection.blank(orderId, clock.instant());
        when(repository.findForUpdate(orderId)).thenReturn(Optional.of(row));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.upsert(orderId, ProjectionSource.ORDER, "CONFIRMED", clock.instant());
        service.upsert(orderId, ProjectionSource.PAYMENT, "APPROVED", clock.instant());
        OrderSummaryProjection afterDispatch =
                service.upsert(orderId, ProjectionSource.DISPATCH, "PREPARING", clock.instant());

        // La fila se crea con ON CONFLICT: dos consumidores a la vez ya no chocan por clave duplicada.
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.times(3)).insertIfAbsent(orderId, clock.instant());
        assertThat(afterDispatch.getOrderStatus()).isEqualTo("CONFIRMED");
        assertThat(afterDispatch.getPaymentStatus()).isEqualTo("APPROVED");
        assertThat(afterDispatch.getDispatchStatus()).isEqualTo("PREPARING");
    }

    @Test
    void anOlderEventArrivingLateDoesNotOverwriteANewerOne() {
        UUID orderId = UUID.randomUUID();
        OrderSummaryProjection row = OrderSummaryProjection.blank(orderId, clock.instant());
        when(repository.findForUpdate(orderId)).thenReturn(Optional.of(row));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.upsert(orderId, ProjectionSource.DISPATCH, "IN_TRANSIT", clock.instant());
        service.upsert(orderId, ProjectionSource.DISPATCH, "PREPARING", clock.instant().minusSeconds(30));

        assertThat(row.getDispatchStatus()).isEqualTo("IN_TRANSIT");
    }

    @Test
    void findByOrderIdFailsExplicitlyWhenNothingIsKnownYet() {
        UUID orderId = UUID.randomUUID();
        when(repository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByOrderId(orderId))
                .isInstanceOf(OrderSummaryNotFoundException.class);
    }

    @Test
    void responseMarksFieldsOlderThanThresholdAsStale() {
        UUID orderId = UUID.randomUUID();
        Instant old = clock.instant().minus(Duration.ofHours(48));
        OrderSummaryProjection projection = OrderSummaryProjection.blank(orderId, old);
        projection.apply(ProjectionSource.PAYMENT, "APPROVED", old, old);

        OrderSummaryResponse response = OrderSummaryResponse.from(projection, clock.instant());

        assertThat(response.paymentStatus().status()).isEqualTo("APPROVED");
        assertThat(response.paymentStatus().stale()).isTrue();
        assertThat(response.orderStatus().status()).isNull();
        assertThat(response.orderStatus().stale()).isFalse();
    }
}
