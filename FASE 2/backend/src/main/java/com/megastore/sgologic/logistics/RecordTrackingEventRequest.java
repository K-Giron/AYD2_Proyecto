package com.megastore.sgologic.logistics;

import jakarta.validation.constraints.NotNull;

/**
 * Simula la notificación de estado que LogiExpress enviaría en producción
 * (CDU-08, paso 4) mientras BB-06 no entrega el simulador oficial.
 */
public record RecordTrackingEventRequest(@NotNull ShipmentStatus status) {
}
