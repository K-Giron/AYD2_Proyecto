package com.megastore.sgologic.logistics;

import java.time.Instant;
import java.util.UUID;

/**
 * Respuesta pública del despacho. Coincide con el resultado mínimo
 * propuesto para el puerto de Saga: {@code shipmentId}, guía y estado.
 */
public record ShipmentResponse(
        UUID shipmentId,
        UUID orderId,
        DispatchOrigin origin,
        String originCode,
        String destination,
        ShipmentStatus status,
        String trackingNumber,
        int labelAttempts,
        UUID correlationId,
        Instant createdAt,
        Instant updatedAt
) {
    static ShipmentResponse from(Shipment shipment) {
        return new ShipmentResponse(
                shipment.getId(),
                shipment.getOrderId(),
                shipment.getOrigin(),
                shipment.getOriginCode(),
                shipment.getDestination(),
                shipment.getStatus(),
                shipment.getTrackingNumber(),
                shipment.getLabelAttempts(),
                shipment.getCorrelationId(),
                shipment.getCreatedAt(),
                shipment.getUpdatedAt()
        );
    }
}
