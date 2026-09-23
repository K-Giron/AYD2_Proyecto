package com.megastore.sgologic.audit.api;

import java.util.UUID;

/**
 * Datos mínimos de una operación auditable (CDU-12): qué agregado cambió, a
 * qué orden pertenece, qué acción se ejecutó, cuál fue el resultado y la
 * correlación del flujo. {@code beforeState} y {@code afterState} son JSON
 * opcionales con el detalle del cambio.
 *
 * <p>No debe contener PAN, CVV ni secretos: la evidencia se conserva y se
 * consulta, y EaC-12 prohíbe almacenar datos de pago completos.</p>
 */
public record AuditEntryRequest(
        String aggregateType,
        UUID aggregateId,
        UUID orderId,
        String action,
        String result,
        String beforeState,
        String afterState,
        UUID correlationId,
        UUID causationId,
        UUID eventId
) {

    public AuditEntryRequest {
        if (aggregateType == null || aggregateType.isBlank()) {
            throw new IllegalArgumentException("aggregateType es obligatorio.");
        }
        if (aggregateId == null) {
            throw new IllegalArgumentException("aggregateId es obligatorio.");
        }
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("action es obligatoria.");
        }
        if (result == null || result.isBlank()) {
            throw new IllegalArgumentException("result es obligatorio.");
        }
        if (correlationId == null) {
            throw new IllegalArgumentException("correlationId es obligatorio.");
        }
    }

    public static AuditEntryRequest of(String aggregateType, UUID aggregateId, UUID orderId,
                                       String action, String result, UUID correlationId) {
        return new AuditEntryRequest(aggregateType, aggregateId, orderId, action, result,
                null, null, correlationId, null, null);
    }
}
