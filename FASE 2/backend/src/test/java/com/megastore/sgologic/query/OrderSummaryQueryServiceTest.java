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
        when(repository.findById(orderId)).thenReturn(Optional.empty(), Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OrderSummaryProjection afterOrder = service.upsert(orderId, ProjectionSource.ORDER, "CONFIRMED", clock.instant());
        when(repository.findById(orderId)).thenReturn(Optional.of(afterOrder));
        OrderSummaryProjection afterDispatch =
                service.upsert(orderId, ProjectionSource.DISPATCH, "PREPARING", clock.instant());

        assertThat(afterDispatch.getOrderStatus()).isEqualTo("CONFIRMED");
        assertThat(afterDispatch.getDispatchStatus()).isEqualTo("PREPARING");
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
