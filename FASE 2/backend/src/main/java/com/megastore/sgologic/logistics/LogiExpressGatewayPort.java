package com.megastore.sgologic.logistics;

import java.util.UUID;

/**
 * Puerto hacia LogiExpress (CDU-08). El adaptador real invocará la interfaz
 * REST del proveedor con timeout definido. En desarrollo usa
 * {@link SimulatedLogiExpressAdapter}, entregado por BB-06.
 */
public interface LogiExpressGatewayPort {

    LogiExpressLabelResult requestLabel(UUID shipmentId, UUID orderId, String destination, UUID correlationId)
            throws LogiExpressTimeoutException, LogiExpressUnavailableException;
}
