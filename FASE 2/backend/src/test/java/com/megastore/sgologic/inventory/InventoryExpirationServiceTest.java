package com.megastore.sgologic.inventory;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de INV-03 sin infraestructura ni planificador real: el
 * constructor no dispara {@code @PostConstruct} en una instanciación
 * directa fuera del contenedor de Spring, así que estas pruebas ejercen
 * {@link InventoryExpirationService#releaseExpiredReservations()}
 * directamente, como lo haría el planificador en producción.
 */
class InventoryExpirationServiceTest {

    private final InventoryReservationRepository reservationRepository = mock(InventoryReservationRepository.class);
    private final InventoryItemRepository itemRepository = mock(InventoryItemRepository.class);
    private final TaskScheduler scheduler = mock(TaskScheduler.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-23T12:00:00Z"), ZoneOffset.UTC);
    // `self` solo lo usa scheduleSweep() (@PostConstruct), que estas pruebas
    // no disparan al instanciar la clase directamente fuera de Spring.
    private final InventoryExpirationService service = new InventoryExpirationService(
            reservationRepository, itemRepository, clock, scheduler, Duration.ofMinutes(1), null);

    private static InventoryReservation activeReservation(UUID id, UUID productId, UUID locationId, int quantity) {
        InventoryReservation reservation = new InventoryReservation();
        ReflectionTestUtils.setField(reservation, "id", id);
        ReflectionTestUtils.setField(reservation, "orderId", UUID.randomUUID());
        ReflectionTestUtils.setField(reservation, "productId", productId);
        ReflectionTestUtils.setField(reservation, "locationId", locationId);
        ReflectionTestUtils.setField(reservation, "quantity", quantity);
        ReflectionTestUtils.setField(reservation, "status", InventoryReservationStatus.ACTIVE);
        return reservation;
    }

    @Test
    void releaseExpiredReservationsReleasesStockAndMarksReservationExpired() {
        UUID reservationId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        InventoryReservation reservation = activeReservation(reservationId, productId, locationId, 2);

        when(reservationRepository.findByStatusAndExpiresAtBefore(
                eq(InventoryReservationStatus.ACTIVE), eq(Instant.parse("2026-09-23T12:00:00Z"))))
                .thenReturn(List.of(reservation));
        when(itemRepository.tryRelease(productId, locationId, 2, Instant.parse("2026-09-23T12:00:00Z")))
                .thenReturn(1);

        int released = service.releaseExpiredReservations();

        assertThat(released).isEqualTo(1);
        assertThat(reservation.getStatus()).isEqualTo(InventoryReservationStatus.EXPIRED);
        verify(itemRepository).tryRelease(productId, locationId, 2, Instant.parse("2026-09-23T12:00:00Z"));
        verify(reservationRepository).saveAndFlush(reservation);
    }

    @Test
    void releaseExpiredReservationsDoesNothingWhenNoneAreExpired() {
        when(reservationRepository.findByStatusAndExpiresAtBefore(any(), any())).thenReturn(List.of());

        int released = service.releaseExpiredReservations();

        assertThat(released).isZero();
        verify(itemRepository, never()).tryRelease(any(), any(), anyInt(), any());
        verify(reservationRepository, never()).saveAndFlush(any());
    }

    /**
     * Regresión: cuando el lote tiene más de una reserva vencida, cada
     * {@code tryRelease} limpia el contexto de persistencia
     * ({@code clearAutomatically = true}), lo que descarta cualquier
     * {@code save()} todavía no volcado a la base de datos de una reserva
     * anterior del mismo lote. Detectado en vivo contra PostgreSQL real
     * (los mocks de este test, por diseño, no simulan ese comportamiento y
     * no lo habrían detectado); el fix es {@code saveAndFlush} en vez de
     * {@code save} dentro del ciclo. Esta prueba fija esa interacción
     * exacta para que una regresión futura sí falle aquí.
     */
    @Test
    void releaseExpiredReservationsFlushesEachReservationBeforeProcessingTheNextOne() {
        UUID productId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        InventoryReservation first = activeReservation(UUID.randomUUID(), productId, locationId, 1);
        InventoryReservation second = activeReservation(UUID.randomUUID(), productId, locationId, 3);

        when(reservationRepository.findByStatusAndExpiresAtBefore(any(), any()))
                .thenReturn(List.of(first, second));
        when(itemRepository.tryRelease(eq(productId), eq(locationId), any(Integer.class), any())).thenReturn(1);

        int released = service.releaseExpiredReservations();

        assertThat(released).isEqualTo(2);
        assertThat(first.getStatus()).isEqualTo(InventoryReservationStatus.EXPIRED);
        assertThat(second.getStatus()).isEqualTo(InventoryReservationStatus.EXPIRED);
        verify(itemRepository, times(2)).tryRelease(eq(productId), eq(locationId), anyInt(), any());
        verify(reservationRepository).saveAndFlush(first);
        verify(reservationRepository).saveAndFlush(second);
    }
}
