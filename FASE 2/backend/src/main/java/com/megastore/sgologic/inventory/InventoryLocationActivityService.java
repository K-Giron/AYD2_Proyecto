package com.megastore.sgologic.inventory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Actividad de una ubicación (kárdex): entradas y salidas registradas y las
 * reservas de venta con su estado — activa (stock comprometido), confirmada
 * (vendida), liberada o vencida por el temporizador. Solo lectura; la usa la
 * tienda online para mostrar su almacén (CD-WEB).
 */
@Service
public class InventoryLocationActivityService {

    private final InventoryLocationRepository locationRepository;
    private final InventoryStockMovementRepository movementRepository;
    private final InventoryReservationRepository reservationRepository;

    public InventoryLocationActivityService(InventoryLocationRepository locationRepository,
                                            InventoryStockMovementRepository movementRepository,
                                            InventoryReservationRepository reservationRepository) {
        this.locationRepository = locationRepository;
        this.movementRepository = movementRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional(readOnly = true)
    public List<LocationActivity> activityAt(String locationCode) {
        InventoryLocation location = locationRepository.findByCode(locationCode)
                .orElseThrow(() -> new InventoryLocationNotFoundException(locationCode));
        List<LocationActivity> activity = new ArrayList<>();
        movementRepository.findTop200ByLocationIdOrderByOccurredAtDesc(location.getId()).forEach(movement ->
                activity.add(new LocationActivity(movement.getType() == StockMovementType.INBOUND ? "ENTRADA" : "SALIDA",
                        movement.getProductId(), movement.getQuantity(), movement.getReason(), null, null,
                        movement.getOccurredAt(), null)));
        reservationRepository.findTop200ByLocationIdOrderByCreatedAtDesc(location.getId()).forEach(reservation ->
                activity.add(new LocationActivity("RESERVA", reservation.getProductId(), reservation.getQuantity(),
                        null, reservation.getStatus().name(), reservation.getOrderId(), reservation.getCreatedAt(),
                        reservation.getExpiresAt())));
        activity.sort(Comparator.comparing(LocationActivity::occurredAt).reversed());
        return activity;
    }

    /**
     * {@code kind}: ENTRADA, SALIDA o RESERVA. Para RESERVA, {@code status} es
     * ACTIVE, CONFIRMED, RELEASED o EXPIRED y {@code expiresAt} el vencimiento
     * del temporizador.
     */
    public record LocationActivity(String kind, UUID productId, int quantity, String reason, String status,
                                   UUID orderId, Instant occurredAt, Instant expiresAt) {
    }
}
