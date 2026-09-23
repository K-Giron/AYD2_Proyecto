package com.megastore.sgologic.payments;

import com.megastore.sgologic.shared.simulation.SimulatorMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulador configurable de PayKlik. Nunca recibe PAN ni CVV: consume una
 * referencia segura o tokenizada, respetando EaC-12.
 */
@Component
public class SimulatedPayKlikAdapter implements PayKlikGatewayPort {

    private final SimulatorMode mode;
    private final long latencyMs;
    private final double failureRate;
    private final Clock clock;
    private final ConcurrentMap<String, AuthorizationResult> completedRequests = new ConcurrentHashMap<>();

    public SimulatedPayKlikAdapter(
            @Value("${sgo.simulators.payklik.mode:SUCCESS}") SimulatorMode mode,
            @Value("${sgo.simulators.payklik.latency-ms:1200}") long latencyMs,
            @Value("${sgo.simulators.payklik.failure-rate:0.10}") double failureRate,
            Clock clock) {
        this.mode = mode;
        this.latencyMs = Math.max(0L, latencyMs);
        this.failureRate = Math.max(0.0, Math.min(1.0, failureRate));
        this.clock = clock;
    }

    @Override
    public AuthorizationResult authorize(AuthorizationRequest request) {
        requireSafeRequest(request);
        return completedRequests.computeIfAbsent(request.idempotencyKey(), ignored -> simulateAuthorization());
    }

    private AuthorizationResult simulateAuthorization() {
        SimulatorMode effectiveMode = effectiveMode();
        if (effectiveMode == SimulatorMode.LATENCY) {
            pause(latencyMs);
        }
        if (effectiveMode == SimulatorMode.TIMEOUT) {
            throw new PayKlikTimeoutException("PayKlik excedio el tiempo de espera configurado.");
        }
        if (effectiveMode == SimulatorMode.OUTAGE) {
            throw new PayKlikUnavailableException("PayKlik no esta disponible.");
        }
        if (effectiveMode == SimulatorMode.REJECTION) {
            return new AuthorizationResult(UUID.randomUUID(), false, null,
                    "PAYMENT_REJECTED", clock.instant());
        }
        return new AuthorizationResult(UUID.randomUUID(), true,
                "PK-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase(),
                null, clock.instant());
    }

    private void requireSafeRequest(AuthorizationRequest request) {
        if (request == null || request.orderId() == null || request.correlationId() == null
                || request.amount() == null || request.amount().signum() <= 0
                || request.currency() == null || request.currency().isBlank()
                || request.paymentReference() == null || request.paymentReference().isBlank()
                || request.idempotencyKey() == null || request.idempotencyKey().isBlank()) {
            throw new IllegalArgumentException("La solicitud PayKlik no contiene todos los datos obligatorios.");
        }
        String compact = request.paymentReference().replace(" ", "");
        if (compact.matches("\\d{13,19}") || compact.toLowerCase().contains("cvv")) {
            throw new IllegalArgumentException("PayKlik solo acepta una referencia segura; PAN/CVV estan prohibidos.");
        }
    }

    private SimulatorMode effectiveMode() {
        if (mode != SimulatorMode.RANDOM) {
            return mode;
        }
        return ThreadLocalRandom.current().nextDouble() < failureRate
                ? SimulatorMode.TIMEOUT : SimulatorMode.SUCCESS;
    }

    private void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new PayKlikUnavailableException("La simulacion de PayKlik fue interrumpida.");
        }
    }
}
