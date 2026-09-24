package com.megastore.sgologic.payments;

import com.megastore.sgologic.shared.simulation.SimulatorMode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimulatedPayKlikAdapterTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-22T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void successModeReturnsProviderReferenceWithoutCardData() {
        SimulatedPayKlikAdapter adapter = new SimulatedPayKlikAdapter(SimulatorMode.SUCCESS, 0, 0, clock);
        PayKlikGatewayPort.AuthorizationRequest request = request("tok_test_123");
        PayKlikGatewayPort.AuthorizationResult result = adapter.authorize(request);
        PayKlikGatewayPort.AuthorizationResult repeated = adapter.authorize(request);

        assertThat(result.approved()).isTrue();
        assertThat(result.providerReference()).startsWith("PK-");
        assertThat(result.processedAt()).isEqualTo(clock.instant());
        assertThat(repeated).isSameAs(result);
    }

    @Test
    void rejectionModeReturnsBusinessRejection() {
        SimulatedPayKlikAdapter adapter = new SimulatedPayKlikAdapter(SimulatorMode.REJECTION, 0, 0, clock);
        PayKlikGatewayPort.AuthorizationResult result = adapter.authorize(request("tok_rejected"));

        assertThat(result.approved()).isFalse();
        assertThat(result.rejectionCode()).isEqualTo("PAYMENT_REJECTED");
    }

    @Test
    void rawPanOrCvvReferenceIsRejectedBeforeCallingProvider() {
        SimulatedPayKlikAdapter adapter = new SimulatedPayKlikAdapter(SimulatorMode.SUCCESS, 0, 0, clock);

        assertThatThrownBy(() -> adapter.authorize(request("4111111111111111")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PAN/CVV");
        assertThatThrownBy(() -> adapter.authorize(request("cvv=123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PAN/CVV");
    }

    private PayKlikGatewayPort.AuthorizationRequest request(String reference) {
        return new PayKlikGatewayPort.AuthorizationRequest(UUID.randomUUID(), new BigDecimal("112.00"),
                "GTQ", reference, UUID.randomUUID(), "payklik-test-key");
    }
}
