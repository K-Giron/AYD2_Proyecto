package com.megastore.sgologic.integration;

import com.megastore.sgologic.shared.simulation.SimulatorMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/** Simula las 32 instalaciones POS sin modificar ni asumir su implementacion. */
@Component
class SimulatedPosGatewayAdapter implements PosGatewayPort {

    private final SimulatorMode mode;
    private final long latencyMs;
    private final double failureRate;
    private final Set<Integer> unavailableStores;

    SimulatedPosGatewayAdapter(
            @Value("${sgo.simulators.pos.mode:SUCCESS}") SimulatorMode mode,
            @Value("${sgo.simulators.pos.latency-ms:400}") long latencyMs,
            @Value("${sgo.simulators.pos.failure-rate:0.05}") double failureRate,
            @Value("${sgo.simulators.pos.unavailable-stores:}") String unavailableStores) {
        this.mode = mode;
        this.latencyMs = Math.max(0L, latencyMs);
        this.failureRate = Math.max(0.0, Math.min(1.0, failureRate));
        this.unavailableStores = parseStores(unavailableStores);
    }

    @Override
    public DeliveryResult deliver(int storeNumber, UUID eventId, String eventType, UUID orderId,
                                  String payload, UUID correlationId) {
        if (storeNumber < 1 || storeNumber > 32) {
            throw new IllegalArgumentException("El POS debe pertenecer a una tienda entre 1 y 32.");
        }
        SimulatorMode effective = unavailableStores.contains(storeNumber)
                ? SimulatorMode.OUTAGE : effectiveMode();
        if (effective == SimulatorMode.LATENCY) { pause(); }
        if (effective == SimulatorMode.TIMEOUT) {
            throw new PosTimeoutException("POS " + storeNumber + " no respondio dentro del timeout.");
        }
        if (effective == SimulatorMode.OUTAGE) {
            throw new PosUnavailableException("POS " + storeNumber + " no esta disponible.");
        }
        if (effective == SimulatorMode.REJECTION) {
            return new DeliveryResult(false, null, "POS_REJECTED_EVENT");
        }
        return new DeliveryResult(true,
                "POS-" + String.format("%02d", storeNumber) + "-ACK-" + eventId, null);
    }

    private SimulatorMode effectiveMode() {
        if (mode != SimulatorMode.RANDOM) { return mode; }
        return ThreadLocalRandom.current().nextDouble() < failureRate
                ? SimulatorMode.TIMEOUT : SimulatorMode.SUCCESS;
    }

    private Set<Integer> parseStores(String value) {
        if (value == null || value.isBlank()) { return Set.of(); }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .map(Integer::valueOf)
                .filter(store -> store >= 1 && store <= 32)
                .collect(Collectors.toUnmodifiableSet());
    }

    private void pause() {
        try { Thread.sleep(latencyMs); }
        catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new PosUnavailableException("La simulacion POS fue interrumpida.");
        }
    }
}
