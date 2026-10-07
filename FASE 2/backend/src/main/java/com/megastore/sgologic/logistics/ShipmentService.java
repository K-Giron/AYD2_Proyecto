package com.megastore.sgologic.logistics;

import com.megastore.sgologic.integration.outbox.OutboxEventRequest;
import com.megastore.sgologic.integration.outbox.PublishEvent;
import com.megastore.sgologic.logistics.api.DispatchPort;
import com.megastore.sgologic.logistics.api.DispatchRequest;
import com.megastore.sgologic.logistics.api.DispatchResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Orquesta LOG-01 (crear despacho desde el origen asignado), LOG-02
 * (guía y tracking mediante LogiExpress simulado) y LOG-03 (reintentos
 * 2/8/32 segundos y DLQ), conforme a CDU-07 y CDU-08.
 *
 * <p>El puerto de Saga ({@code saga-required-ports.md}) que Kevin es dueño
 * de proveer recibe {@code orderId, origen, destino, correlationId,
 * idempotencyKey} y responde {@code shipmentId, guía, estado}; este
 * servicio implementa exactamente esa forma.</p>
 */
@Service
public class ShipmentService implements DispatchPort {

    private static final Logger log = LoggerFactory.getLogger(ShipmentService.class);

    /** Reintentos de guía LogiExpress en 2, 8 y 32 segundos (LOG-03). */
    static final long[] RETRY_DELAYS_SECONDS = {2L, 8L, 32L};

    /** Tipo de evento de dominio publicado por LOG-01/02 (CUN-11: notificaciones de despacho). */
    static final String SHIPMENT_STATUS_CHANGED = "SHIPMENT_STATUS_CHANGED";
    private static final String AGGREGATE_TYPE = "SHIPMENT";

    private final ShipmentRepository shipmentRepository;
    private final ShipmentTrackingEventRepository trackingEventRepository;
    private final ShipmentDeadLetterRepository deadLetterRepository;
    private final LogiExpressGatewayPort logiExpressGateway;
    private final TaskScheduler retryScheduler;
    private final PublishEvent eventPublisher;
    private final Clock clock;

    public ShipmentService(ShipmentRepository shipmentRepository,
                            ShipmentTrackingEventRepository trackingEventRepository,
                            ShipmentDeadLetterRepository deadLetterRepository,
                            LogiExpressGatewayPort logiExpressGateway,
                            @Qualifier("logisticsRetryScheduler") TaskScheduler retryScheduler,
                            PublishEvent eventPublisher,
                            Clock clock) {
        this.shipmentRepository = shipmentRepository;
        this.trackingEventRepository = trackingEventRepository;
        this.deadLetterRepository = deadLetterRepository;
        this.logiExpressGateway = logiExpressGateway;
        this.retryScheduler = retryScheduler;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    /**
     * LOG-01: crea el despacho para el origen ya asignado a la orden. Es
     * idempotente por {@code idempotencyKey}: una solicitud repetida
     * devuelve el despacho existente en vez de duplicarlo.
     */
    @Transactional
    public Shipment createDispatch(UUID orderId, DispatchOrigin origin, String destination,
                                    UUID correlationId, String idempotencyKey) {
        return createDispatch(orderId, origin, null, destination, correlationId, idempotencyKey);
    }

    /** Igual que el anterior, indicando además el almacén concreto de origen ({@code originCode}). */
    @Transactional
    public Shipment createDispatch(UUID orderId, DispatchOrigin origin, String originCode, String destination,
                                   UUID correlationId, String idempotencyKey) {
        Optional<Shipment> existing = shipmentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Solicitud de despacho repetida para orderId={} con idempotencyKey conocida; "
                    + "se devuelve el despacho existente sin duplicar.", orderId);
            return existing.get();
        }

        Shipment shipment = Shipment.assign(orderId, origin, originCode, destination, correlationId, idempotencyKey,
                clock.instant());
        shipmentRepository.save(shipment);
        publishStatusChanged(shipment, "PREPARING", correlationId);

        if (shipment.requiresExternalLabel()) {
            scheduleLabelRequestAfterCommit(shipment.getId());
        }
        return shipment;
    }

    /**
     * Programa la primera solicitud de guía solo después de que la
     * transacción que creó el despacho confirme: si se programara dentro de
     * la transacción, el hilo del planificador podría ejecutarse antes del
     * commit y no encontrar el despacho todavía (visible solo para su propia
     * transacción). Mismo patrón que {@code NotificationService.dispatchAfterCommit}.
     */
    private void scheduleLabelRequestAfterCommit(UUID shipmentId) {
        Runnable schedule = () -> retryScheduler.schedule(() -> requestLabelWithRetry(shipmentId), Instant.now());
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { schedule.run(); }
            });
        } else {
            schedule.run();
        }
    }

    /**
     * CUN-11: notifica únicamente los tres hechos de despacho que
     * Notifications traduce a aviso del cliente (preparación, tránsito y
     * entrega); los estados internos de la guía (LABEL_REQUESTED,
     * LABEL_ISSUED, LABEL_FAILED) no generan ruido al cliente.
     */
    private void publishStatusChanged(Shipment shipment, String notifiableStatus, UUID correlationId) {
        publishStatusChanged(shipment, notifiableStatus, correlationId, "SGO-LOGIC");
    }

    /** {@code source}: quién reportó el hecho (LOGIEXPRESS, la tienda de retiro, la consola). */
    private void publishStatusChanged(Shipment shipment, String notifiableStatus, UUID correlationId, String source) {
        if (!("PREPARING".equals(notifiableStatus) || "IN_TRANSIT".equals(notifiableStatus)
                || "READY_FOR_PICKUP".equals(notifiableStatus) || "DELIVERED".equals(notifiableStatus))) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("shipmentId", shipment.getId());
        payload.put("orderId", shipment.getOrderId());
        payload.put("status", notifiableStatus);
        payload.put("trackingNumber", shipment.getTrackingNumber());
        payload.put("destination", shipment.getDestination());
        payload.put("source", source);
        eventPublisher.publish(new OutboxEventRequest(AGGREGATE_TYPE, shipment.getId(),
                SHIPMENT_STATUS_CHANGED, 1, correlationId, correlationId, payload));
    }

    /**
     * LOG-02 / CDU-08: solicita la guía a LogiExpress. Si falla por timeout
     * o indisponibilidad, reintenta con backoff 2/8/32 s (LOG-03) sin
     * bloquear la preparación física del pedido (CDU-07, E1). Al agotar los
     * reintentos, registra el mensaje en la cola de fallidos local.
     */
    void requestLabelWithRetry(UUID shipmentId) {
        Optional<Shipment> maybeShipment = shipmentRepository.findById(shipmentId);
        if (maybeShipment.isEmpty()) {
            log.warn("Se intentó solicitar guía para un despacho inexistente: {}", shipmentId);
            return;
        }
        Shipment shipment = maybeShipment.get();
        if (shipment.getTrackingNumber() != null) {
            // La guía ya existe (la respuesta tardía la trajo el rastreo): no se vuelve a pedir.
            return;
        }
        shipment.markLabelRequested(clock.instant());
        // Reasignar la copia gestionada: conserva la version vigente para el
        // siguiente save y evita el bloqueo optimista del objeto desconectado.
        shipment = shipmentRepository.save(shipment);

        try {
            LogiExpressLabelResult result = logiExpressGateway.requestLabel(shipment.getId(), shipment.getOrderId(),
                    originLabel(shipment), shipment.getDestination(), shipment.getCorrelationId());
            shipment.markLabelIssued(result.trackingNumber(), clock.instant());
            shipmentRepository.save(shipment);
            trackingEventRepository.save(ShipmentTrackingEvent.of(
                    shipmentId, ShipmentStatus.LABEL_ISSUED, "LOGIEXPRESS", clock.instant()));
        } catch (LogiExpressTimeoutException | LogiExpressUnavailableException failure) {
            handleLabelFailure(shipment, failure.getMessage());
        }
    }

    private void handleLabelFailure(Shipment shipment, String reason) {
        Shipment current = shipmentRepository.findById(shipment.getId()).orElse(shipment);
        if (current.getTrackingNumber() != null) {
            // Mientras esperábamos, el rastreo de LogiExpress asoció la guía: la falla ya no aplica.
            return;
        }
        int attemptsSoFar = shipment.getLabelAttempts();
        int nextRetryIndex = attemptsSoFar - 1;
        if (nextRetryIndex < RETRY_DELAYS_SECONDS.length) {
            long delaySeconds = RETRY_DELAYS_SECONDS[nextRetryIndex];
            UUID shipmentId = shipment.getId();
            log.warn("Guía de LogiExpress falló (intento {}) para el despacho {}: {}. Reintentando en {} s.",
                    attemptsSoFar, shipmentId, reason, delaySeconds);
            retryScheduler.schedule(() -> requestLabelWithRetry(shipmentId),
                    Instant.now().plusSeconds(delaySeconds));
        } else {
            log.error("Se agotaron los reintentos de LogiExpress para el despacho {}: {}", shipment.getId(), reason);
            shipment.markLabelFailed(clock.instant());
            shipmentRepository.save(shipment);
            deadLetterRepository.save(ShipmentDeadLetter.of(shipment.getId(), reason, attemptsSoFar, clock.instant()));
        }
    }

    /**
     * LOG-02 / CDU-08, paso 4: registra un estado posterior comunicado por
     * LogiExpress (o, en recogida, por el flujo interno de tienda).
     */
    @Transactional
    public Shipment recordTrackingEvent(UUID shipmentId, ShipmentStatus newStatus, String source) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ShipmentNotFoundException(shipmentId));
        shipment.applyTrackingStatus(newStatus, clock.instant());
        shipmentRepository.save(shipment);
        trackingEventRepository.save(ShipmentTrackingEvent.of(shipmentId, newStatus, source, clock.instant()));
        publishStatusChanged(shipment, newStatus.name(), shipment.getCorrelationId(), source);
        return shipment;
    }

    /**
     * Conciliación con LogiExpress: si la guía se emitió pero la respuesta no
     * llegó (timeout), el rastreo la trae después y se asocia al despacho.
     */
    @Transactional
    public boolean reconcileLabel(UUID shipmentId, String trackingNumber) {
        Shipment shipment = shipmentRepository.findById(shipmentId).orElse(null);
        if (shipment == null || (shipment.getTrackingNumber() != null && shipment.getStatus() != ShipmentStatus.LABEL_FAILED)) {
            return false;
        }
        shipment.markLabelIssued(trackingNumber, clock.instant());
        shipmentRepository.save(shipment);
        trackingEventRepository.save(ShipmentTrackingEvent.of(shipmentId, ShipmentStatus.LABEL_ISSUED,
                "LOGIEXPRESS-CONCILIADA", clock.instant()));
        return true;
    }

    /**
     * Retiro en tienda: el cliente recoge en la tienda que eligió el pedido que
     * LogiExpress ya dejó ahí. Solo esa tienda puede entregarlo, y solo una vez.
     */
    @Transactional
    public Shipment handOverAtStore(UUID orderId, String storeCode, String receivedBy) {
        Shipment shipment = getShipmentsForOrder(orderId).stream().findFirst()
                .orElseThrow(() -> new PickupHandoffException("La orden " + orderId + " no tiene despacho."));
        if (shipment.getDestination() == null || !shipment.getDestination().contains("(" + storeCode + ")")) {
            throw new PickupHandoffException("Este pedido no se recoge en la tienda " + storeCode + ".");
        }
        if (shipment.getStatus() == ShipmentStatus.DELIVERED) {
            throw new PickupHandoffException("El pedido ya fue entregado al cliente.");
        }
        if (shipment.getStatus() != ShipmentStatus.READY_FOR_PICKUP) {
            throw new PickupHandoffException("El pedido todavía no llega a la tienda (estado " + shipment.getStatus() + ").");
        }
        log.info("Pedido {} entregado al cliente en {} (recibió {}).", orderId, storeCode, receivedBy);
        return recordTrackingEvent(shipment.getId(), ShipmentStatus.DELIVERED, "TIENDA " + storeCode);
    }

    /**
     * LOG-03: un despacho cuya guía quedó en la bandeja de fallidos (LogiExpress
     * estuvo caído más que los reintentos) se vuelve a solicitar con 2/8/32 s.
     */
    @Transactional
    public Shipment retryLabel(UUID shipmentId) {
        Shipment shipment = getShipment(shipmentId);
        if (shipment.getStatus() != ShipmentStatus.LABEL_FAILED || shipment.getTrackingNumber() != null) {
            return shipment;
        }
        shipment.reopenLabelRequest(clock.instant());
        shipment = shipmentRepository.save(shipment);
        trackingEventRepository.save(ShipmentTrackingEvent.of(shipmentId, ShipmentStatus.LABEL_REQUESTED,
                "CONSOLA-REINTENTO", clock.instant()));
        scheduleLabelRequestAfterCommit(shipmentId);
        return shipment;
    }

    private static String originLabel(Shipment shipment) {
        if (shipment.getOriginCode() != null) {
            return (shipment.getOrigin() == DispatchOrigin.STORE ? "Tienda " : "Almacén ") + shipment.getOriginCode();
        }
        return switch (shipment.getOrigin()) {
            case DISTRIBUTION_CENTER -> "Centro de distribución MegaStore";
            case STORE -> "Tienda MegaStore";
            case CUSTOMER_PICKUP -> "Retiro del cliente";
        };
    }

    /**
     * CDU-07, A1: el cliente cambia el método de entrega después de
     * confirmada la compra. Este incremento reevalúa únicamente el destino
     * y el origen de despacho; la reevaluación de la reserva de inventario
     * queda pendiente de INV-04 (Jorge Pumay).
     */
    @Transactional
    public Shipment changeDeliveryMethod(UUID shipmentId, DispatchOrigin newOrigin, String newDestination) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ShipmentNotFoundException(shipmentId));
        Shipment reassigned = Shipment.assign(shipment.getOrderId(), newOrigin, newDestination,
                shipment.getCorrelationId(), shipment.getIdempotencyKey() + ":reassigned:" + UUID.randomUUID(),
                clock.instant());
        shipmentRepository.save(reassigned);
        publishStatusChanged(reassigned, "PREPARING", reassigned.getCorrelationId());
        if (reassigned.requiresExternalLabel()) {
            scheduleLabelRequestAfterCommit(reassigned.getId());
        }
        return reassigned;
    }

    public Shipment getShipment(UUID shipmentId) {
        return shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ShipmentNotFoundException(shipmentId));
    }

    /** Vista por orden para consolidar despacho junto a las demás áreas (CDU-09). */
    public List<Shipment> getShipmentsForOrder(UUID orderId) {
        return shipmentRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
    }

    public List<ShipmentTrackingEvent> getTrackingHistory(UUID shipmentId) {
        return trackingEventRepository.findByShipmentIdOrderByOccurredAtAsc(shipmentId);
    }

    /**
     * {@code @Transactional} propio: aunque delega en el método de 5
     * argumentos por autoinvocación (que Spring no intercepta), esta
     * anotación abre la transacción ambiente que {@link #publishStatusChanged}
     * necesita cuando la Saga llama por esta ruta en vez de por el REST
     * directo (donde el proxy sí aplica la transacción del otro método).
     */
    @Override
    @Transactional
    public DispatchResult createDispatch(DispatchRequest request) {
        DispatchOrigin origin = DispatchOrigin.valueOf(request.origin());
        Shipment shipment = createDispatch(request.orderId(), origin, request.originCode(), request.destination(),
                request.correlationId(), request.idempotencyKey());
        return toResult(shipment);
    }

    @Override
    public Optional<DispatchResult> latestForOrder(UUID orderId) {
        return getShipmentsForOrder(orderId).stream().findFirst().map(this::toResult);
    }

    private DispatchResult toResult(Shipment shipment) {
        return new DispatchResult(shipment.getId(), shipment.getOrderId(),
                shipment.getStatus().name(), shipment.getTrackingNumber());
    }
}
