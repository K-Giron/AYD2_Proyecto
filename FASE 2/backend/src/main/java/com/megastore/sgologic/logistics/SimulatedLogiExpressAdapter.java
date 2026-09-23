package com.megastore.sgologic.logistics;

import com.megastore.sgologic.shared.simulation.SimulatorMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Simulador BB-06 configurable de LogiExpress, con timeout aleatorio de 2 % por defecto. */
@Component
class SimulatedLogiExpressAdapter implements LogiExpressGatewayPort {

    private final SimulatorMode mode;
    private final long latencyMs;
    private final double timeoutRate;

    SimulatedLogiExpressAdapter(
            @Value("${sgo.simulators.logiexpress.mode:RANDOM}") SimulatorMode mode,
            @Value("${sgo.simulators.logiexpress.latency-ms:800}") long latencyMs,
            @Value("${sgo.simulators.logiexpress.failure-rate:0.02}") double timeoutRate) {
        this.mode = mode;
        this.latencyMs = Math.max(0L, latencyMs);
        this.timeoutRate = Math.max(0.0, Math.min(1.0, timeoutRate));
    }

    @Override
    public LogiExpressLabelResult requestLabel(UUID shipmentId, UUID orderId, String destination, UUID correlationId)
            throws LogiExpressTimeoutException, LogiExpressUnavailableException {
        SimulatorMode effectiveMode = mode == SimulatorMode.RANDOM
                && ThreadLocalRandom.current().nextDouble() < timeoutRate
                ? SimulatorMode.TIMEOUT : mode == SimulatorMode.RANDOM ? SimulatorMode.SUCCESS : mode;
        if (effectiveMode == SimulatorMode.LATENCY) {
            pause();
        }
        if (effectiveMode == SimulatorMode.OUTAGE || effectiveMode == SimulatorMode.REJECTION) {
            throw new LogiExpressUnavailableException("LogiExpress no esta disponible para el despacho " + shipmentId);
        }
        if (effectiveMode == SimulatorMode.TIMEOUT) {
            throw new LogiExpressTimeoutException(
                    "LogiExpress no confirmó la guía dentro del timeout para el despacho " + shipmentId);
        }
        String trackingNumber = "LGX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new LogiExpressLabelResult(trackingNumber);
    }

    private void pause() throws LogiExpressUnavailableException {
        try {
            Thread.sleep(latencyMs);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new LogiExpressUnavailableException("La simulacion de LogiExpress fue interrumpida.");
        }
    }
}
