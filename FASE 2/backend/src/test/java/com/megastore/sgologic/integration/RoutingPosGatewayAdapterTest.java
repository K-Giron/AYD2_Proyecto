package com.megastore.sgologic.integration;

import com.megastore.sgologic.shared.simulation.SimulatorMode;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoutingPosGatewayAdapterTest {

    private RoutingPosGatewayAdapter adapter(Map<Integer, String> endpoints) {
        PosStoreEndpointsProperties properties = new PosStoreEndpointsProperties();
        properties.setStoreEndpoints(endpoints);
        properties.setTimeoutMs(500);
        return new RoutingPosGatewayAdapter(new SimulatedPosGatewayAdapter(SimulatorMode.SUCCESS, 0, 0.0, ""), properties);
    }

    @Test
    void storesWithoutARealSystemUseTheSimulator() {
        PosGatewayPort.DeliveryResult result = adapter(Map.of()).deliver(7, UUID.randomUUID(), "ORDER_CONFIRMED",
                UUID.randomUUID(), "{}", UUID.randomUUID());

        assertThat(result.accepted()).isTrue();
    }

    @Test
    void unreachableStoreSystemIsANetworkFailureToRetry() {
        assertThatThrownBy(() -> adapter(Map.of(1, "http://127.0.0.1:9/api/store/pos-events"))
                .deliver(1, UUID.randomUUID(), "ORDER_CONFIRMED", UUID.randomUUID(), "{}", UUID.randomUUID()))
                .isInstanceOf(PosGatewayPort.PosUnavailableException.class);
    }
}
