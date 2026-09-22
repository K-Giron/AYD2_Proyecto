package com.megastore.sgologic.logistics;

import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Adaptador temporal de LogiExpress mientras BB-06 (simuladores
 * configurables de Andy Sánchez para PayKlik, SAT, ERP, POS y LogiExpress)
 * no está disponible. Reproduce la tasa de timeout descrita en el DDA
 * (§2.3: "aproximadamente una de cada 50 solicitudes termina en timeout",
 * ~2 %) para que LOG-03 pueda ejercitarse de forma realista. Debe
 * sustituirse por el simulador oficial de BB-06 cuando Andy lo entregue.
 */
@Component
class SimulatedLogiExpressAdapter implements LogiExpressGatewayPort {

    private static final double TIMEOUT_RATE = 0.02;

    @Override
    public LogiExpressLabelResult requestLabel(UUID shipmentId, UUID orderId, String destination, UUID correlationId)
            throws LogiExpressTimeoutException {
        if (ThreadLocalRandom.current().nextDouble() < TIMEOUT_RATE) {
            throw new LogiExpressTimeoutException(
                    "LogiExpress no confirmó la guía dentro del timeout para el despacho " + shipmentId);
        }
        String trackingNumber = "LGX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new LogiExpressLabelResult(trackingNumber);
    }
}
