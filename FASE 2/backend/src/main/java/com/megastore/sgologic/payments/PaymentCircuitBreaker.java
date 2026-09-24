package com.megastore.sgologic.payments;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

/** Circuit breaker mínimo y determinista para aislar una caída de PayKlik. */
@Component
class PaymentCircuitBreaker {
    private final int threshold;
    private final long openSeconds;
    private final Clock clock;
    private int failures;
    private Instant openUntil;

    PaymentCircuitBreaker(@Value("${sgo.payments.circuit-breaker.failure-threshold:3}") int threshold,
                          @Value("${sgo.payments.circuit-breaker.open-seconds:30}") long openSeconds,
                          Clock clock) {
        this.threshold = Math.max(1, threshold);
        this.openSeconds = Math.max(1, openSeconds);
        this.clock = clock;
    }

    synchronized boolean permitsCall() {
        if (openUntil == null) return true;
        if (!clock.instant().isBefore(openUntil)) {
            openUntil = null;
            failures = 0;
            return true;
        }
        return false;
    }

    synchronized void success() {
        failures = 0;
        openUntil = null;
    }

    synchronized void failure() {
        failures++;
        if (failures >= threshold) openUntil = clock.instant().plusSeconds(openSeconds);
    }
}
