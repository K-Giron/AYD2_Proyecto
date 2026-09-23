package com.megastore.sgologic.inventory;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * INV-03: libera automáticamente las reservas activas cuyo {@code expiresAt}
 * ya pasó (CDU-02, postcondición: "Una reserva no concluida se libera en un
 * tiempo acorde con la operación comercial"). Usa la misma primitiva
 * atómica que INV-02 ({@link InventoryItemRepository#tryRelease}) para
 * devolver la cantidad a disponible.
 *
 * <p><b>Auditoría:</b> el DDA (EaC-07) exige poder reconstruir quién,
 * cuándo y qué cambió. El registro de auditoría transversal (AUD-01,
 * Edson) y el Outbox (BB-04) todavía no existen en este incremento; hasta
 * que estén disponibles, cada liberación queda registrada únicamente en el
 * log estructurado de la aplicación con el {@code reservationId},
 * {@code orderId}, producto y ubicación. Esta es una limitación conocida,
 * no una omisión silenciosa: cuando AUD-01/BB-04 estén listos, este método
 * debe publicar el evento correspondiente en vez de (o además de) loguear.</p>
 */
@Service
public class InventoryExpirationService {

    private static final Logger log = LoggerFactory.getLogger(InventoryExpirationService.class);

    private final InventoryReservationRepository reservationRepository;
    private final InventoryItemRepository itemRepository;
    private final Clock clock;
    private final TaskScheduler scheduler;
    private final Duration sweepInterval;
    private final InventoryExpirationService self;

    public InventoryExpirationService(InventoryReservationRepository reservationRepository,
                                       InventoryItemRepository itemRepository,
                                       Clock clock,
                                       @Qualifier("inventoryExpirationScheduler") TaskScheduler scheduler,
                                       @Value("${sgo-logic.inventory.expiration-sweep-interval:PT1M}")
                                       Duration sweepInterval,
                                       @Lazy InventoryExpirationService self) {
        this.reservationRepository = reservationRepository;
        this.itemRepository = itemRepository;
        this.clock = clock;
        this.scheduler = scheduler;
        this.sweepInterval = sweepInterval;
        this.self = self;
    }

    /**
     * Programa el barrido periódico al arrancar la aplicación. Intervalo
     * configurable mediante {@code sgo-logic.inventory.expiration-sweep-interval}
     * (por defecto un minuto); la primera ejecución ocurre de inmediato.
     *
     * <p>Programa {@code self::releaseExpiredReservations}, no
     * {@code this::...}: capturar {@code this} referencia el objeto crudo,
     * fuera del proxy de Spring, y {@code @Transactional} dejaría de
     * aplicarse silenciosamente (fallaría en producción con
     * {@code TransactionRequiredException} en la primera ejecución del
     * barrido). {@code self} es una auto-inyección perezosa del propio bean
     * proxied, el patrón estándar de Spring para este caso.</p>
     */
    @PostConstruct
    void scheduleSweep() {
        scheduler.scheduleAtFixedRate(self::releaseExpiredReservations, sweepInterval);
    }

    /**
     * Libera todas las reservas {@code ACTIVE} vencidas al momento de la
     * llamada. Público para permitir invocarlo manualmente (pruebas,
     * demostración) además de por el planificador.
     */
    @Transactional
    public int releaseExpiredReservations() {
        Instant now = clock.instant();
        List<InventoryReservation> expired = reservationRepository
                .findByStatusAndExpiresAtBefore(InventoryReservationStatus.ACTIVE, now);

        for (InventoryReservation reservation : expired) {
            int releasedRows = itemRepository.tryRelease(
                    reservation.getProductId(), reservation.getLocationId(), reservation.getQuantity(), now);
            reservation.markExpired(now);
            // saveAndFlush, no save: el tryRelease de la SIGUIENTE reserva
            // en este mismo lote también limpia el contexto de persistencia
            // (clearAutomatically=true), y esa limpieza descarta cualquier
            // save() todavía no volcado a la base de datos. Sin este flush
            // inmediato, solo la última reserva del lote queda persistida
            // como EXPIRED aunque el log reporte éxito para todas.
            reservationRepository.saveAndFlush(reservation);

            if (releasedRows == 1) {
                log.info("INV-03: reserva {} (orden {}) expirada; liberada(s) {} unidad(es) de producto {} en ubicacion {}",
                        reservation.getId(), reservation.getOrderId(), reservation.getQuantity(),
                        reservation.getProductId(), reservation.getLocationId());
            } else {
                log.warn("INV-03: reserva {} marcada como expirada pero tryRelease no afecto filas; "
                        + "revisar consistencia del item de inventario", reservation.getId());
            }
        }
        return expired.size();
    }
}
