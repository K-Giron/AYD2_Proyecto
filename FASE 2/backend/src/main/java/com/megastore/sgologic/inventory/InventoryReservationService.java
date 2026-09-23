package com.megastore.sgologic.inventory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * INV-02: implementa el puerto de reserva de inventario que la Saga
 * (ORD-03) requerirá (CDU-02, {@code saga-required-ports.md}).
 *
 * <p><b>Atomicidad:</b> la decisión de "hay stock o no" se resuelve con una
 * única sentencia {@code UPDATE ... WHERE} ({@link InventoryItemRepository#tryReserve})
 * en vez de un patrón leer-decidir-escribir en Java. Esto es lo que
 * garantiza CDU-02 E1: si dos solicitudes por la última unidad llegan casi
 * al mismo tiempo, PostgreSQL serializa las dos escrituras sobre la misma
 * fila y solo una de las dos sentencias UPDATE afecta una fila.</p>
 *
 * <p><b>Idempotencia:</b> una repetición con la misma {@code idempotencyKey}
 * devuelve la reserva ya existente en vez de reservar de nuevo. La
 * restricción UNIQUE de {@code inventory_reservations.idempotency_key} es
 * la red de seguridad a nivel de base de datos; el caso límite de dos
 * solicitudes con la MISMA clave exactamente simultáneas (ninguna ve
 * todavía el registro de la otra al consultar) queda documentado como
 * limitación conocida: la segunda recibirá un error de integridad en vez
 * de la reserva existente. Es un escenario distinto del que CDU-02 E1
 * exige cubrir (dos claves DISTINTAS compitiendo por la última unidad, que
 * sí se garantiza correctamente) y se resolvería con reintento en el
 * cliente o una tabla de deduplicación previa, fuera del alcance de
 * INV-02.</p>
 */
@Service
public class InventoryReservationService {

    /**
     * ADR-014: centros de distribución antes que tiendas; dentro del mismo
     * tipo, por código ascendente. Determinista y explicable, no representa
     * cercanía real al cliente (el DDA no define esos datos).
     */
    private static final Comparator<InventoryLocation> ORIGIN_PRIORITY = Comparator
            .<InventoryLocation>comparingInt(
                    location -> location.getType() == InventoryLocationType.DISTRIBUTION_CENTER ? 0 : 1)
            .thenComparing(InventoryLocation::getCode);

    private final InventoryItemRepository itemRepository;
    private final InventoryLocationRepository locationRepository;
    private final InventoryReservationRepository reservationRepository;
    private final Clock clock;
    private final Duration reservationTtl;

    public InventoryReservationService(InventoryItemRepository itemRepository,
                                        InventoryLocationRepository locationRepository,
                                        InventoryReservationRepository reservationRepository,
                                        Clock clock,
                                        @Value("${sgo-logic.inventory.reservation-ttl:PT15M}") Duration reservationTtl) {
        this.itemRepository = itemRepository;
        this.locationRepository = locationRepository;
        this.reservationRepository = reservationRepository;
        this.clock = clock;
        this.reservationTtl = reservationTtl;
    }

    @Transactional
    public InventoryReservation reserve(UUID orderId, UUID productId, UUID locationId, int quantity,
                                         UUID correlationId, String idempotencyKey) {
        Optional<InventoryReservation> existing = reservationRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        Instant now = clock.instant();
        int reservedRows = itemRepository.tryReserve(productId, locationId, quantity, now);
        if (reservedRows == 0) {
            boolean itemExists = itemRepository.findByProductIdAndLocationId(productId, locationId).isPresent();
            if (!itemExists) {
                throw new InventoryItemNotFoundException(productId, locationId);
            }
            throw new InsufficientStockException(productId, locationId, quantity);
        }

        InventoryReservation reservation = InventoryReservation.create(
                orderId, productId, locationId, quantity, correlationId, idempotencyKey,
                now.plus(reservationTtl), now);
        return reservationRepository.save(reservation);
    }

    /**
     * INV-04 / ADR-014: como {@link #reserve}, pero sin ubicación conocida.
     * Recorre las ubicaciones con stock registrado para el producto en el
     * orden de {@link #ORIGIN_PRIORITY} e intenta la misma sentencia
     * atómica de {@link #reserve} en cada una hasta que una tenga éxito.
     * No hay ventana entre "elegir" y "reservar": cada intento es atómico
     * por sí mismo, así que si la candidata preferida pierde una carrera
     * concurrente, simplemente se continúa con la siguiente.
     */
    @Transactional
    public InventoryReservation reserveAtBestOrigin(UUID orderId, UUID productId, int quantity,
                                                      UUID correlationId, String idempotencyKey) {
        Optional<InventoryReservation> existing = reservationRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        List<InventoryItem> candidates = itemRepository.findByProductIdOrderByLocationId(productId);
        Map<UUID, InventoryLocation> locationsById = locationRepository.findAllById(
                        candidates.stream().map(InventoryItem::getLocationId).toList()).stream()
                .collect(Collectors.toMap(InventoryLocation::getId, location -> location));

        List<UUID> orderedLocationIds = candidates.stream()
                .map(InventoryItem::getLocationId)
                .sorted(Comparator.comparing(locationsById::get, ORIGIN_PRIORITY))
                .toList();

        Instant now = clock.instant();
        for (UUID locationId : orderedLocationIds) {
            if (itemRepository.tryReserve(productId, locationId, quantity, now) == 1) {
                InventoryReservation reservation = InventoryReservation.create(
                        orderId, productId, locationId, quantity, correlationId, idempotencyKey,
                        now.plus(reservationTtl), now);
                return reservationRepository.save(reservation);
            }
        }
        throw new NoAvailableOriginException(productId, quantity);
    }

    public InventoryReservation getReservation(UUID reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new InventoryReservationNotFoundException(reservationId));
    }
}
