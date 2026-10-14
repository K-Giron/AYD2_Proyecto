package com.megastore.sgologic.inventory;

import com.megastore.sgologic.integration.outbox.OutboxEventRequest;
import com.megastore.sgologic.integration.outbox.PublishEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Entradas y salidas de inventario fuera del flujo de reserva de venta en línea. */
class InventoryMovementServiceTest {

    private final InventoryItemRepository itemRepository = mock(InventoryItemRepository.class);
    private final InventoryLocationRepository locationRepository = mock(InventoryLocationRepository.class);
    private final InventoryStockMovementRepository movementRepository = mock(InventoryStockMovementRepository.class);
    private final PublishEvent eventPublisher = mock(PublishEvent.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-24T10:00:00Z"), ZoneOffset.UTC);
    private final InventoryMovementService service = new InventoryMovementService(
            itemRepository, locationRepository, movementRepository, eventPublisher, clock);

    private static InventoryLocation location(UUID id, InventoryLocationType type) {
        InventoryLocation location = new InventoryLocation();
        ReflectionTestUtils.setField(location, "id", id);
        ReflectionTestUtils.setField(location, "code", "ST-05");
        ReflectionTestUtils.setField(location, "type", type);
        return location;
    }

    private static InventoryItem item(UUID id, UUID productId, UUID locationId, int quantityOnHand) {
        InventoryItem item = new InventoryItem();
        ReflectionTestUtils.setField(item, "id", id);
        ReflectionTestUtils.setField(item, "productId", productId);
        ReflectionTestUtils.setField(item, "locationId", locationId);
        ReflectionTestUtils.setField(item, "quantityOnHand", quantityOnHand);
        return item;
    }

    @Test
    void registerMovementIncreasesOnHandForInboundAndPublishesEvent() {
        UUID productId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        when(locationRepository.findById(locationId))
                .thenReturn(Optional.of(location(locationId, InventoryLocationType.STORE)));
        when(itemRepository.tryIncreaseOnHand(productId, locationId, 10, clock.instant())).thenReturn(1);
        when(itemRepository.findByProductIdAndLocationId(productId, locationId))
                .thenReturn(Optional.of(item(itemId, productId, locationId, 30)));
        when(movementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        InventoryStockMovement movement = service.registerMovement(
                productId, locationId, StockMovementType.INBOUND, 10, "Reabastecimiento", correlationId);

        assertThat(movement.getType()).isEqualTo(StockMovementType.INBOUND);
        assertThat(movement.getQuantity()).isEqualTo(10);
        verify(itemRepository).tryIncreaseOnHand(productId, locationId, 10, clock.instant());
        verify(itemRepository, never()).tryDecreaseOnHand(any(), any(), anyInt(), any());

        ArgumentCaptor<OutboxEventRequest> event = ArgumentCaptor.forClass(OutboxEventRequest.class);
        verify(eventPublisher).publish(event.capture());
        assertThat(event.getValue().eventType()).isEqualTo(InventoryMovementService.STOCK_MOVEMENT_REGISTERED);
        assertThat(event.getValue().aggregateId()).isEqualTo(itemId);
    }

    @Test
    void registerMovementDecreasesOnHandForOutbound() {
        UUID productId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        when(locationRepository.findById(locationId))
                .thenReturn(Optional.of(location(locationId, InventoryLocationType.STORE)));
        when(itemRepository.tryDecreaseOnHand(productId, locationId, 3, clock.instant())).thenReturn(1);
        when(itemRepository.findByProductIdAndLocationId(productId, locationId))
                .thenReturn(Optional.of(item(itemId, productId, locationId, 7)));
        when(movementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        InventoryStockMovement movement = service.registerMovement(
                productId, locationId, StockMovementType.OUTBOUND, 3, "Merma", UUID.randomUUID());

        assertThat(movement.getType()).isEqualTo(StockMovementType.OUTBOUND);
        verify(itemRepository).tryDecreaseOnHand(productId, locationId, 3, clock.instant());
    }

    @Test
    void registerMovementThrowsInsufficientOnHandStockWhenOutboundExceedsAvailability() {
        UUID productId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        when(locationRepository.findById(locationId))
                .thenReturn(Optional.of(location(locationId, InventoryLocationType.STORE)));
        when(itemRepository.tryDecreaseOnHand(productId, locationId, 100, clock.instant())).thenReturn(0);
        when(itemRepository.findByProductIdAndLocationId(productId, locationId))
                .thenReturn(Optional.of(item(UUID.randomUUID(), productId, locationId, 5)));

        assertThatThrownBy(() -> service.registerMovement(
                productId, locationId, StockMovementType.OUTBOUND, 100, null, UUID.randomUUID()))
                .isInstanceOf(InsufficientOnHandStockException.class);

        verify(movementRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void registerMovementThrowsLocationNotFoundWhenLocationDoesNotExist() {
        UUID locationId = UUID.randomUUID();
        when(locationRepository.findById(locationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registerMovement(
                UUID.randomUUID(), locationId, StockMovementType.INBOUND, 1, null, UUID.randomUUID()))
                .isInstanceOf(InventoryLocationNotFoundException.class);
    }
}
