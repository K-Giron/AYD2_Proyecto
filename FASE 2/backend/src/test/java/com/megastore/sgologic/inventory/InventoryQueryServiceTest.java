package com.megastore.sgologic.inventory;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pruebas de INV-01 sin infraestructura: los repositorios se sustituyen por
 * dobles. Las entidades se construyen con {@link ReflectionTestUtils}
 * porque {@link InventoryItem} e {@link InventoryLocation} solo exponen un
 * constructor protegido para JPA (no crean estado válido fuera de una
 * migración o de la reserva atómica de INV-02).
 */
class InventoryQueryServiceTest {

    private final InventoryItemRepository itemRepository = mock(InventoryItemRepository.class);
    private final InventoryLocationRepository locationRepository = mock(InventoryLocationRepository.class);
    private final InventoryQueryService service = new InventoryQueryService(itemRepository, locationRepository);

    private static InventoryLocation location(UUID id, String code, InventoryLocationType type) {
        InventoryLocation location = new InventoryLocation();
        ReflectionTestUtils.setField(location, "id", id);
        ReflectionTestUtils.setField(location, "code", code);
        ReflectionTestUtils.setField(location, "name", "Ubicación " + code);
        ReflectionTestUtils.setField(location, "type", type);
        ReflectionTestUtils.setField(location, "createdAt", Instant.parse("2026-09-10T00:00:00Z"));
        return location;
    }

    private static InventoryItem item(UUID productId, UUID locationId, int onHand, int reserved) {
        InventoryItem item = new InventoryItem();
        ReflectionTestUtils.setField(item, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(item, "productId", productId);
        ReflectionTestUtils.setField(item, "locationId", locationId);
        ReflectionTestUtils.setField(item, "quantityOnHand", onHand);
        ReflectionTestUtils.setField(item, "quantityReserved", reserved);
        ReflectionTestUtils.setField(item, "updatedAt", Instant.parse("2026-09-10T00:00:00Z"));
        return item;
    }

    @Test
    void getStockByProductAndLocationComputesAvailableAsOnHandMinusReserved() {
        UUID productId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        InventoryLocation cd01 = location(locationId, "CD-01", InventoryLocationType.DISTRIBUTION_CENTER);
        InventoryItem stock = item(productId, locationId, 5, 2);

        when(locationRepository.findById(locationId)).thenReturn(Optional.of(cd01));
        when(itemRepository.findByProductIdAndLocationId(productId, locationId)).thenReturn(Optional.of(stock));

        InventoryStockResponse response = service.getStockByProductAndLocation(productId, locationId);

        assertThat(response.quantityOnHand()).isEqualTo(5);
        assertThat(response.quantityReserved()).isEqualTo(2);
        assertThat(response.quantityAvailable()).isEqualTo(3);
        assertThat(response.locationCode()).isEqualTo("CD-01");
    }

    @Test
    void getStockByProductAndLocationThrowsWhenNoItemRegistered() {
        UUID productId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        when(locationRepository.findById(locationId))
                .thenReturn(Optional.of(location(locationId, "ST-01", InventoryLocationType.STORE)));
        when(itemRepository.findByProductIdAndLocationId(productId, locationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStockByProductAndLocation(productId, locationId))
                .isInstanceOf(InventoryItemNotFoundException.class);
    }

    @Test
    void getStockByProductAndLocationThrowsWhenLocationDoesNotExist() {
        UUID productId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        when(locationRepository.findById(locationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStockByProductAndLocation(productId, locationId))
                .isInstanceOf(InventoryItemNotFoundException.class);
    }

    @Test
    void getStockByProductReturnsOneRowPerLocationWithStockRegistered() {
        UUID productId = UUID.randomUUID();
        UUID cd01Id = UUID.randomUUID();
        UUID st01Id = UUID.randomUUID();
        InventoryItem stockAtCd01 = item(productId, cd01Id, 5, 0);
        InventoryItem stockAtSt01 = item(productId, st01Id, 1, 0);

        when(itemRepository.findByProductIdOrderByLocationId(productId))
                .thenReturn(List.of(stockAtCd01, stockAtSt01));
        when(locationRepository.findAllById(any())).thenReturn(List.of(
                location(cd01Id, "CD-01", InventoryLocationType.DISTRIBUTION_CENTER),
                location(st01Id, "ST-01", InventoryLocationType.STORE)));

        List<InventoryStockResponse> stock = service.getStockByProduct(productId);

        assertThat(stock).hasSize(2)
                .extracting(InventoryStockResponse::locationCode)
                .containsExactlyInAnyOrder("CD-01", "ST-01");
    }

    @Test
    void getLocationsDelegatesToRepositoryOrderedByCode() {
        UUID id = UUID.randomUUID();
        when(locationRepository.findAllByOrderByCodeAsc())
                .thenReturn(List.of(location(id, "CD-01", InventoryLocationType.DISTRIBUTION_CENTER)));

        List<InventoryLocation> locations = service.getLocations();

        assertThat(locations).extracting(InventoryLocation::getCode).containsExactly("CD-01");
    }
}
