package com.megastore.sgologic.inventory;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * INV-01: consulta de stock por producto y por ubicación. Es la única forma
 * en que otros módulos pueden conocer disponibilidad (ADR-011, ADR-001):
 * nunca acceden a {@link InventoryItemRepository} directamente.
 */
@Service
public class InventoryQueryService {

    private final InventoryItemRepository itemRepository;
    private final InventoryLocationRepository locationRepository;

    public InventoryQueryService(InventoryItemRepository itemRepository,
                                  InventoryLocationRepository locationRepository) {
        this.itemRepository = itemRepository;
        this.locationRepository = locationRepository;
    }

    public List<InventoryLocation> getLocations() {
        return locationRepository.findAllByOrderByCodeAsc();
    }

    /** Disponibilidad de un producto en todas las ubicaciones donde tiene stock registrado. */
    public List<InventoryStockResponse> getStockByProduct(UUID productId) {
        List<InventoryItem> items = itemRepository.findByProductIdOrderByLocationId(productId);
        Map<UUID, InventoryLocation> locationsById = locationRepository.findAllById(
                        items.stream().map(InventoryItem::getLocationId).toList()).stream()
                .collect(Collectors.toMap(InventoryLocation::getId, location -> location));

        return items.stream()
                .map(item -> InventoryStockResponse.from(item, locationsById.get(item.getLocationId())))
                .toList();
    }

    /**
     * Disponibilidad de un producto en una ubicación concreta. Lanza
     * {@link InventoryItemNotFoundException} si no hay stock registrado
     * para esa combinación, en vez de asumir cero: distingue "sin stock
     * conocido" de "cero unidades", que son casos distintos para CDU-02.
     */
    public InventoryStockResponse getStockByProductAndLocation(UUID productId, UUID locationId) {
        InventoryLocation location = locationRepository.findById(locationId)
                .orElseThrow(() -> new InventoryItemNotFoundException(productId, locationId));
        InventoryItem item = itemRepository.findByProductIdAndLocationId(productId, locationId)
                .orElseThrow(() -> new InventoryItemNotFoundException(productId, locationId));
        return InventoryStockResponse.from(item, location);
    }
}
