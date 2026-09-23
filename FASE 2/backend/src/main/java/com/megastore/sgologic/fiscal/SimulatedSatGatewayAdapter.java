package com.megastore.sgologic.fiscal;

import com.megastore.sgologic.shared.simulation.SimulatorMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Simulador SAT de BB-06 con exito, rechazo, latencia, timeout y caida. */
@Component
public class SimulatedSatGatewayAdapter implements SatGatewayPort {

    private final SimulatorMode mode;
    private final long latencyMs;
    private final double failureRate;

    public SimulatedSatGatewayAdapter(
            @Value("${sgo.simulators.sat.mode:SUCCESS}") SimulatorMode mode,
            @Value("${sgo.simulators.sat.latency-ms:1500}") long latencyMs,
            @Value("${sgo.simulators.sat.failure-rate:0.10}") double failureRate) {
        this.mode = mode;
        this.latencyMs = Math.max(0L, latencyMs);
        this.failureRate = Math.max(0.0, Math.min(1.0, failureRate));
    }

    @Override
    public SatResult submit(SatSubmission submission) {
        if (submission == null || submission.fiscalDocumentId() == null || submission.orderId() == null
                || submission.correlationId() == null || submission.cui() == null
                || submission.series() == null || submission.xmlPayload() == null
                || !submission.xmlPayload().contains("<DTE")) {
            throw new IllegalArgumentException("La solicitud SAT no contiene un DTE valido.");
        }
        SimulatorMode effectiveMode = effectiveMode();
        if (effectiveMode == SimulatorMode.LATENCY) {
            pause();
        }
        if (effectiveMode == SimulatorMode.TIMEOUT) {
            throw new SatTimeoutException("SAT no respondio dentro del tiempo de espera configurado.");
        }
        if (effectiveMode == SimulatorMode.OUTAGE) {
            throw new SatUnavailableException("SAT no esta disponible.");
        }
        if (effectiveMode == SimulatorMode.REJECTION) {
            return new SatResult(false, null, "SAT_REJECTED",
                    "El documento fue rechazado por el simulador SAT.");
        }
        return new SatResult(true,
                "SAT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(),
                null, "Documento autorizado.");
    }

    private SimulatorMode effectiveMode() {
        if (mode != SimulatorMode.RANDOM) {
            return mode;
        }
        return ThreadLocalRandom.current().nextDouble() < failureRate
                ? SimulatorMode.TIMEOUT : SimulatorMode.SUCCESS;
    }

    private void pause() {
        try {
            Thread.sleep(latencyMs);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new SatUnavailableException("La simulacion SAT fue interrumpida.");
        }
    }
}
