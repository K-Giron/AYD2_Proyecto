package com.megastore.sgologic.logistics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Rastreo automático (CDU-08, paso 4; LOG-02): cada pocos segundos pide a
 * LogiExpress las guías que cambiaron y aplica su estado al despacho. El
 * cambio publica {@code SHIPMENT_STATUS_CHANGED}, que mueve la orden, avisa
 * al cliente y a las tiendas. Idempotente: solo avanza, nunca retrocede, y
 * también asocia guías emitidas cuya respuesta se perdió por timeout.
 */
@Component
@ConditionalOnExpression("!'${sgo.integrations.logiexpress.base-url:}'.isBlank()")
@ConditionalOnProperty(name = "sgo.integrations.logiexpress.tracking-enabled", havingValue = "true", matchIfMissing = true)
class LogiExpressTrackingSync {

    private static final Logger log = LoggerFactory.getLogger(LogiExpressTrackingSync.class);

    /** Avance de un despacho: el rastreo solo lo mueve hacia adelante. */
    private static final Map<ShipmentStatus, Integer> PROGRESS = Map.of(
            ShipmentStatus.IN_TRANSIT, 1, ShipmentStatus.READY_FOR_PICKUP, 2, ShipmentStatus.DELIVERED, 3,
            ShipmentStatus.RETURNED, 3);
    private static final Map<String, ShipmentStatus> CARRIER_STATUS = Map.of(
            "EN_TRANSITO", ShipmentStatus.IN_TRANSIT,
            "EN_PUNTO_DE_RETIRO", ShipmentStatus.READY_FOR_PICKUP,
            "ENTREGADA", ShipmentStatus.DELIVERED);

    private final LogiExpressTrackingPort tracking;
    private final ShipmentService shipments;
    private volatile Instant cursor = Instant.EPOCH;

    LogiExpressTrackingSync(LogiExpressTrackingPort tracking, ShipmentService shipments) {
        this.tracking = tracking;
        this.shipments = shipments;
    }

    @Scheduled(fixedDelayString = "${sgo.integrations.logiexpress.tracking-interval-ms:10000}",
            initialDelayString = "${sgo.integrations.logiexpress.tracking-initial-delay-ms:15000}")
    void poll() {
        try {
            List<LogiExpressTrackingPort.TrackedGuide> guides = tracking.guidesUpdatedSince(cursor);
            for (LogiExpressTrackingPort.TrackedGuide guide : guides) {
                apply(guide);
                if (guide.updatedAt() != null && guide.updatedAt().isAfter(cursor)) {
                    cursor = guide.updatedAt();
                }
            }
        } catch (LogiExpressUnavailableException offline) {
            log.warn("Rastreo de LogiExpress no disponible; se reintenta en el siguiente ciclo: {}", offline.getMessage());
        }
    }

    void apply(LogiExpressTrackingPort.TrackedGuide guide) {
        UUID shipmentId;
        try {
            shipmentId = UUID.fromString(guide.merchantReference());
        } catch (IllegalArgumentException notOurs) {
            return;
        }
        Shipment shipment;
        try {
            shipment = shipments.getShipment(shipmentId);
        } catch (ShipmentNotFoundException unknown) {
            return;
        }
        if (shipments.reconcileLabel(shipmentId, guide.trackingNumber())) {
            log.info("Guía {} asociada al despacho {} por rastreo (su respuesta se había perdido).",
                    guide.trackingNumber(), shipmentId);
        }
        ShipmentStatus target = CARRIER_STATUS.get(guide.status());
        if (target == null) {
            return;
        }
        int current = PROGRESS.getOrDefault(shipment.getStatus(), 0);
        if (PROGRESS.get(target) <= current) {
            return;
        }
        // Un despacho que LogiExpress entregó sin que se viera el tránsito queda con su historia completa.
        if (current == 0 && target != ShipmentStatus.IN_TRANSIT) {
            shipments.recordTrackingEvent(shipmentId, ShipmentStatus.IN_TRANSIT, "LOGIEXPRESS");
        }
        shipments.recordTrackingEvent(shipmentId, target, "LOGIEXPRESS");
    }
}
