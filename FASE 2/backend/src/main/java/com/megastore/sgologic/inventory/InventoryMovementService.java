package com.megastore.sgologic.inventory;

import com.megastore.sgologic.integration.outbox.OutboxEventRequest;
import com.megastore.sgologic.integration.outbox.PublishEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Entradas y salidas de inventario fuera del flujo de reserva de venta en
 * línea: recepción de mercadería, merma y ajustes de tienda/POS. Distinto
 * de {@link InventoryReservationService}, que solo comprometido/confirma
 * unidades ya reservadas para una orden.
 */
@Service
public class InventoryMovementService {

    private static final String AGGREGATE_TYPE = "InventoryItem";
    static final String STOCK_MOVEMENT_REGISTERED = "INVENTORY_STOCK_MOVEMENT_REGISTERED";

    private final InventoryItemRepository itemRepository;
    private final InventoryLocationRepository locationRepository;
    private final InventoryStockMovementRepository movementRepository;
    private final PublishEvent eventPublisher;
    private final Clock clock;

    public InventoryMovementService(InventoryItemRepository itemRepository,
                                     InventoryLocationRepository locationRepository,
                                     InventoryStockMovementRepository movementRepository,
                                     PublishEvent eventPublisher,
                                     Clock clock) {
        this.itemRepository = itemRepository;
        this.locationRepository = locationRepository;
        this.movementRepository = movementRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public InventoryStockMovement registerMovement(UUID productId, UUID locationId, StockMovementType type,
                                                     int quantity, String reason, UUID correlationId) {
        InventoryLocation location = locationRepository.findById(locationId)
                .orElseThrow(() -> new InventoryLocationNotFoundException(locationId));
        Instant now = clock.instant();

        int updatedRows = type == StockMovementType.INBOUND
                ? itemRepository.tryIncreaseOnHand(productId, locationId, quantity, now)
                : itemRepository.tryDecreaseOnHand(productId, locationId, quantity, now);

        if (updatedRows == 0) {
            InventoryItem item = itemRepository.findByProductIdAndLocationId(productId, locationId)
                    .orElseThrow(() -> new InventoryItemNotFoundException(productId, locationId));
            if (type == StockMovementType.OUTBOUND) {
                throw new InsufficientOnHandStockException(productId, locationId, quantity);
            }
            // INBOUND solo devuelve 0 filas si la combinación producto/ubicación no existe.
            throw new InventoryItemNotFoundException(item.getProductId(), item.getLocationId());
        }

        InventoryStockMovement movement = movementRepository.save(
                InventoryStockMovement.register(productId, locationId, type, quantity, reason, correlationId, now));

        InventoryItem updatedItem = itemRepository.findByProductIdAndLocationId(productId, locationId)
                .orElseThrow(() -> new InventoryItemNotFoundException(productId, locationId));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("movementId", movement.getId());
        payload.put("productId", productId);
        payload.put("locationId", locationId);
        payload.put("locationType", location.getType().name());
        payload.put("type", type.name());
        payload.put("quantity", quantity);
        payload.put("quantityOnHandAfter", updatedItem.getQuantityOnHand());
        eventPublisher.publish(new OutboxEventRequest(AGGREGATE_TYPE, updatedItem.getId(), STOCK_MOVEMENT_REGISTERED,
                1, correlationId, correlationId, payload));

        return movement;
    }
}
