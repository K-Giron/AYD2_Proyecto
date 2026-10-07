package com.megastore.sgologic.logistics;

import java.util.UUID;

/**
 * Puerto hacia LogiExpress (CDU-08). {@link HttpLogiExpressAdapter} invoca la
 * API REST del proveedor ({@code logiexpress-service}) con timeout definido;
 * sin URL configurada (pruebas automáticas) se usa {@link SimulatedLogiExpressAdapter}.
 */
public interface LogiExpressGatewayPort {

    LogiExpressLabelResult requestLabel(UUID shipmentId, UUID orderId, String destination, UUID correlationId)
            throws LogiExpressTimeoutException, LogiExpressUnavailableException;

    /** Igual que el anterior, indicando el origen físico de la recolección (p. ej. "Almacén CD-WEB"). */
    default LogiExpressLabelResult requestLabel(UUID shipmentId, UUID orderId, String origin, String destination,
                                                UUID correlationId)
            throws LogiExpressTimeoutException, LogiExpressUnavailableException {
        return requestLabel(shipmentId, orderId, destination, correlationId);
    }
}
