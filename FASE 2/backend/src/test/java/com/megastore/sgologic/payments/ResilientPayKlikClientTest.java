package com.megastore.sgologic.payments;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PAY-02 / ADR-003: el límite de tiempo corta una latencia excesiva y el
 * circuit breaker abre tras la racha de fallas sin llamar al proveedor.
 */
class ResilientPayKlikClientTest {

    @Test
    void timesOutWhenProviderExceedsThreshold() {
        PayKlikResilienceProperties properties = new PayKlikResilienceProperties();
        properties.setTimeoutMs(150L);
        PayKlikGatewayPort slowProvider = request -> {
            try {
                Thread.sleep(1_000L);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            return approved();
        };
        ResilientPayKlikClient client = new ResilientPayKlikClient(slowProvider, properties);
        try {
            assertThatThrownBy(() -> client.authorize(request()))
                    .isInstanceOf(PayKlikGatewayPort.PayKlikTimeoutException.class);
        } finally {
            client.shutdownExecutor();
        }
    }

    @Test
    void opensCircuitAfterConfiguredFailuresWithoutCallingProvider() {
        PayKlikResilienceProperties properties = new PayKlikResilienceProperties();
        properties.setMinimumNumberOfCalls(2);
        properties.setSlidingWindowSize(2);
        properties.setFailureRateThreshold(50f);
        properties.setWaitDurationInOpenState(java.time.Duration.ofSeconds(60));
        AtomicInteger providerCalls = new AtomicInteger();
        PayKlikGatewayPort failingProvider = request -> {
            providerCalls.incrementAndGet();
            throw new PayKlikGatewayPort.PayKlikUnavailableException("PayKlik caído.");
        };
        ResilientPayKlikClient client = new ResilientPayKlikClient(failingProvider, properties);
        try {
            for (int i = 0; i < 2; i++) {
                assertThatThrownBy(() -> client.authorize(request()))
                        .isInstanceOf(PayKlikGatewayPort.PayKlikUnavailableException.class);
            }
            assertThat(client.circuitState()).isEqualTo("OPEN");
            int callsBeforeOpen = providerCalls.get();

            assertThatThrownBy(() -> client.authorize(request()))
                    .isInstanceOf(PayKlikCircuitOpenException.class);
            assertThat(providerCalls.get()).isEqualTo(callsBeforeOpen);
        } finally {
            client.shutdownExecutor();
        }
    }

    @Test
    void returnsProviderResultWhenCallSucceeds() {
        PayKlikResilienceProperties properties = new PayKlikResilienceProperties();
        ResilientPayKlikClient client = new ResilientPayKlikClient(request -> approved(), properties);
        try {
            assertThat(client.authorize(request()).approved()).isTrue();
            assertThat(client.circuitState()).isEqualTo("CLOSED");
        } finally {
            client.shutdownExecutor();
        }
    }

    private PayKlikGatewayPort.AuthorizationResult approved() {
        return new PayKlikGatewayPort.AuthorizationResult(UUID.randomUUID(), true, "PK-TEST", null,
                Instant.now(Clock.systemUTC()));
    }

    private PayKlikGatewayPort.AuthorizationRequest request() {
        return new PayKlikGatewayPort.AuthorizationRequest(UUID.randomUUID(), new BigDecimal("150.00"),
                "GTQ", "tok_test_123", UUID.randomUUID(), "idem-" + UUID.randomUUID());
    }
}
