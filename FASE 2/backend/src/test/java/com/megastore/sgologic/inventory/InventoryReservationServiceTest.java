package com.megastore.sgologic.inventory;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de negocio de INV-02 sin infraestructura: los repositorios se
 * sustituyen por dobles. La garantía de atomicidad real bajo concurrencia
 * depende de que {@code tryReserve} se ejecute como una sola sentencia SQL
 * en PostgreSQL (ver Javadoc de {@link InventoryItemRepository#tryReserve});
 * esa prueba con contención real es responsabilidad de INV-05
 * (Testcontainers, hilos concurrentes), no de esta clase.
 */
class InventoryReservationServiceTest {

    private final InventoryItemRepository itemRepository = mock(InventoryItemRepository.class);
    private final InventoryLocationRepository locationRepository = mock(InventoryLocationRepository.class);
    private final InventoryReservationRepository reservationRepository = mock(InventoryReservationRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-23T10:00:00Z"), ZoneOffset.UTC);
    private final InventoryReservationService service = new InventoryReservationService(
            itemRepository, locationRepository, reservationRepository, clock, Duration.ofMinutes(15));

    private static InventoryReservation reservation(UUID id, String idempotencyKey) {
        InventoryReservation reservation = new InventoryReservation();
        ReflectionTestUtils.setField(reservation, "id", id);
        ReflectionTestUtils.setField(reservation, "idempotencyKey", idempotencyKey);
        ReflectionTestUtils.setField(reservation, "status", InventoryReservationStatus.ACTIVE);
        return reservation;
    }

    private static InventoryLocation location(UUID id, String code, InventoryLocationType type) {
        InventoryLocation location = new InventoryLocation();
        ReflectionTestUtils.setField(location, "id", id);
        ReflectionTestUtils.setField(location, "code", code);
        ReflectionTestUtils.setField(location, "type", type);
        return location;
    }

    private static InventoryItem item(UUID productId, UUID locationId) {
        InventoryItem item = new InventoryItem();
        ReflectionTestUtils.setField(item, "productId", productId);
        ReflectionTestUtils.setField(item, "locationId", locationId);
        return item;
    }

    @Test
    void reserveCreatesActiveReservationWhenStockIsSufficient() {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();

        when(reservationRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(itemRepository.tryReserve(eq(productId), eq(locationId), eq(1), any())).thenReturn(1);
        when(reservationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        InventoryReservation result = service.reserve(orderId, productId, locationId, 1, correlationId, "idem-1");

        assertThat(result.getStatus()).isEqualTo(InventoryReservationStatus.ACTIVE);
        assertThat(result.getExpiresAt()).isEqualTo(Instant.parse("2026-09-23T10:15:00Z"));
        verify(itemRepository).tryReserve(productId, locationId, 1, Instant.parse("2026-09-23T10:00:00Z"));
    }

    @Test
    void reserveIsIdempotentAndDoesNotTouchStockOnReplay() {
        UUID productId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        InventoryReservation existing = reservation(UUID.randomUUID(), "idem-1");
        when(reservationRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.of(existing));

        InventoryReservation result = service.reserve(
                UUID.randomUUID(), productId, locationId, 1, UUID.randomUUID(), "idem-1");

        assertThat(result).isSameAs(existing);
        verify(itemRepository, never()).tryReserve(any(), any(), anyInt(), any());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void reserveThrowsInsufficientStockWhenItemExistsButHasNoAvailability() {
        UUID productId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        when(reservationRepository.findByIdempotencyKey("idem-2")).thenReturn(Optional.empty());
        when(itemRepository.tryReserve(eq(productId), eq(locationId), eq(1), any())).thenReturn(0);
        when(itemRepository.findByProductIdAndLocationId(productId, locationId))
                .thenReturn(Optional.of(mock(InventoryItem.class)));

        assertThatThrownBy(() -> service.reserve(
                UUID.randomUUID(), productId, locationId, 1, UUID.randomUUID(), "idem-2"))
                .isInstanceOf(InsufficientStockException.class);

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void reserveThrowsItemNotFoundWhenCombinationDoesNotExist() {
        UUID productId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        when(reservationRepository.findByIdempotencyKey("idem-3")).thenReturn(Optional.empty());
        when(itemRepository.tryReserve(eq(productId), eq(locationId), eq(1), any())).thenReturn(0);
        when(itemRepository.findByProductIdAndLocationId(productId, locationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reserve(
                UUID.randomUUID(), productId, locationId, 1, UUID.randomUUID(), "idem-3"))
                .isInstanceOf(InventoryItemNotFoundException.class);
    }

    @Test
    void getReservationThrowsWhenIdDoesNotExist() {
        UUID reservationId = UUID.randomUUID();
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getReservation(reservationId))
                .isInstanceOf(InventoryReservationNotFoundException.class);
    }

    @Test
    void reserveAtBestOriginPrefersDistributionCenterOverStoreRegardlessOfOrder() {
        UUID productId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID dcId = UUID.randomUUID();
        // Orden de llegada deliberadamente "tienda primero" para probar que
        // el criterio de ADR-014 lo reordena, no el orden del repositorio.
        when(reservationRepository.findByIdempotencyKey("idem-origin-1")).thenReturn(Optional.empty());
        when(itemRepository.findByProductIdOrderByLocationId(productId))
                .thenReturn(List.of(item(productId, storeId), item(productId, dcId)));
        when(locationRepository.findAllById(any())).thenReturn(List.of(
                location(storeId, "ST-05", InventoryLocationType.STORE),
                location(dcId, "CD-02", InventoryLocationType.DISTRIBUTION_CENTER)));
        when(itemRepository.tryReserve(eq(productId), eq(dcId), eq(1), any())).thenReturn(1);
        when(reservationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        InventoryReservation result = service.reserveAtBestOrigin(
                UUID.randomUUID(), productId, 1, UUID.randomUUID(), "idem-origin-1");

        assertThat(result.getLocationId()).isEqualTo(dcId);
        verify(itemRepository, never()).tryReserve(eq(productId), eq(storeId), anyInt(), any());
    }

    @Test
    void reserveAtBestOriginFallsBackToNextCandidateWhenPreferredHasNoStock() {
        UUID productId = UUID.randomUUID();
        UUID dc01Id = UUID.randomUUID();
        UUID dc02Id = UUID.randomUUID();
        when(reservationRepository.findByIdempotencyKey("idem-origin-2")).thenReturn(Optional.empty());
        when(itemRepository.findByProductIdOrderByLocationId(productId))
                .thenReturn(List.of(item(productId, dc01Id), item(productId, dc02Id)));
        when(locationRepository.findAllById(any())).thenReturn(List.of(
                location(dc01Id, "CD-01", InventoryLocationType.DISTRIBUTION_CENTER),
                location(dc02Id, "CD-02", InventoryLocationType.DISTRIBUTION_CENTER)));
        // CD-01 va primero por código, pero pierde la carrera (0 filas afectadas).
        when(itemRepository.tryReserve(eq(productId), eq(dc01Id), eq(1), any())).thenReturn(0);
        when(itemRepository.tryReserve(eq(productId), eq(dc02Id), eq(1), any())).thenReturn(1);
        when(reservationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        InventoryReservation result = service.reserveAtBestOrigin(
                UUID.randomUUID(), productId, 1, UUID.randomUUID(), "idem-origin-2");

        assertThat(result.getLocationId()).isEqualTo(dc02Id);
    }

    @Test
    void reserveAtBestOriginThrowsWhenNoCandidateHasStock() {
        UUID productId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        when(reservationRepository.findByIdempotencyKey("idem-origin-3")).thenReturn(Optional.empty());
        when(itemRepository.findByProductIdOrderByLocationId(productId))
                .thenReturn(List.of(item(productId, locationId)));
        when(locationRepository.findAllById(any()))
                .thenReturn(List.of(location(locationId, "ST-01", InventoryLocationType.STORE)));
        when(itemRepository.tryReserve(eq(productId), eq(locationId), eq(1), any())).thenReturn(0);

        assertThatThrownBy(() -> service.reserveAtBestOrigin(
                UUID.randomUUID(), productId, 1, UUID.randomUUID(), "idem-origin-3"))
                .isInstanceOf(NoAvailableOriginException.class);

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void reserveAtBestOriginIsIdempotent() {
        InventoryReservation existing = reservation(UUID.randomUUID(), "idem-origin-4");
        when(reservationRepository.findByIdempotencyKey("idem-origin-4")).thenReturn(Optional.of(existing));

        InventoryReservation result = service.reserveAtBestOrigin(
                UUID.randomUUID(), UUID.randomUUID(), 1, UUID.randomUUID(), "idem-origin-4");

        assertThat(result).isSameAs(existing);
        verify(itemRepository, never()).findByProductIdOrderByLocationId(any());
    }
}
