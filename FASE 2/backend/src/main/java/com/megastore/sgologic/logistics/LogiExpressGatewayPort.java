package com.megastore.sgologic.logistics;

import java.util.UUID;

/**
 * Puerto hacia LogiExpress (CDU-08). El adaptador real invocará la interfaz
 * REST del proveedor con timeout definido; mientras BB-06 (simuladores
 * configurables de Andy) no esté disponible, este módulo usa
 * {@link SimulatedLogiExpressAdapter} como implementación temporal,
 * documentada como tal.
 */
public interface LogiExpressGatewayPort {

    LogiExpressLabelResult requestLabel(UUID shipmentId, UUID orderId, String destination, UUID correlationId)
            throws LogiExpressTimeoutException, LogiExpressUnavailableException;
}
