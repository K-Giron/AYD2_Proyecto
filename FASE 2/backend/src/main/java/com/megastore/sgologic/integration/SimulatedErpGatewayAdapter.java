package com.megastore.sgologic.integration;

import com.megastore.sgologic.shared.simulation.SimulatorMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Component
class SimulatedErpGatewayAdapter implements ErpGatewayPort {

    private final SimulatorMode mode;
    private final long latencyMs;
    private final double failureRate;

    SimulatedErpGatewayAdapter(
            @Value("${sgo.simulators.erp.mode:SUCCESS}") SimulatorMode mode,
            @Value("${sgo.simulators.erp.latency-ms:1000}") long latencyMs,
            @Value("${sgo.simulators.erp.failure-rate:0.05}") double failureRate) {
        this.mode = mode;
        this.latencyMs = Math.max(0L, latencyMs);
        this.failureRate = Math.max(0.0, Math.min(1.0, failureRate));
    }

    @Override
    public DeliveryResult deliver(UUID batchId, ErpExportFormat format, String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("ERP no acepta archivos vacios.");
        }
        SimulatorMode effective = effectiveMode();
        if (effective == SimulatorMode.LATENCY) { pause(); }
        if (effective == SimulatorMode.TIMEOUT) {
            throw new ErpTimeoutException("ERP-Core no confirmo el lote dentro del timeout.");
        }
        if (effective == SimulatorMode.OUTAGE) {
            throw new ErpUnavailableException("ERP-Core no esta disponible.");
        }
        if (effective == SimulatorMode.REJECTION) {
            return new DeliveryResult(false, null, "ERP_REJECTED_FILE");
        }
        return new DeliveryResult(true, "ERP-ACK-" + batchId, null);
    }

    private SimulatorMode effectiveMode() {
        if (mode != SimulatorMode.RANDOM) { return mode; }
        return ThreadLocalRandom.current().nextDouble() < failureRate
                ? SimulatorMode.TIMEOUT : SimulatorMode.SUCCESS;
    }

    private void pause() {
        try { Thread.sleep(latencyMs); }
        catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new ErpUnavailableException("La simulacion ERP fue interrumpida.");
        }
    }
}
