package com.megastore.sgologic.query;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Vista consolidada de CDU-09. Cada campo con dato conocido incluye
 * {@code stale}: si la última actualización supera {@link #STALE_AFTER}, se
 * devuelve igualmente (CDU-09, E1: "muestra la última información conocida
 * indicando que puede no estar actualizada, en vez de fallar toda la
 * consulta") marcada como posiblemente desactualizada.
 */
public record OrderSummaryResponse(
        UUID orderId,
        FieldView orderStatus,
        FieldView paymentStatus,
        FieldView dispatchStatus,
        FieldView deliveryStatus,
        FieldView incidentStatus,
        Instant updatedAt
) {
    static final Duration STALE_AFTER = Duration.ofHours(24);

    static OrderSummaryResponse from(OrderSummaryProjection projection, Instant now) {
        return new OrderSummaryResponse(
                projection.getOrderId(),
                FieldView.of(projection.getOrderStatus(), projection.getOrderStatusAsOf(), now),
                FieldView.of(projection.getPaymentStatus(), projection.getPaymentStatusAsOf(), now),
                FieldView.of(projection.getDispatchStatus(), projection.getDispatchStatusAsOf(), now),
                FieldView.of(projection.getDeliveryStatus(), projection.getDeliveryStatusAsOf(), now),
                FieldView.of(projection.getIncidentStatus(), projection.getIncidentStatusAsOf(), now),
                projection.getUpdatedAt()
        );
    }

    public record FieldView(String status, Instant asOf, boolean stale) {
        static FieldView of(String status, Instant asOf, Instant now) {
            if (status == null || asOf == null) {
                return new FieldView(null, null, false);
            }
            boolean stale = Duration.between(asOf, now).compareTo(STALE_AFTER) > 0;
            return new FieldView(status, asOf, stale);
        }
    }
}
