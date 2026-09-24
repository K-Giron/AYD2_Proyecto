package com.megastore.sgologic.integration;

import com.megastore.sgologic.shared.testing.ImmediateTaskScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PosIntegrationServiceTest {
    private final Map<UUID, PosDelivery> deliveries = new HashMap<>();
    private final PosDeliveryRepository repository = mock(PosDeliveryRepository.class);
    private final PosGatewayPort gateway = mock(PosGatewayPort.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-22T12:00:00Z"), ZoneOffset.UTC);
    private final PosIntegrationService service = new PosIntegrationService(repository, gateway,
            Runnable::run, new ImmediateTaskScheduler(), clock);

    @BeforeEach
    void setUpRepository() {
        when(repository.save(any())).thenAnswer(invocation -> {
            PosDelivery delivery = invocation.getArgument(0);
            deliveries.put(delivery.getId(), delivery);
            return delivery;
        });
        when(repository.findById(any())).thenAnswer(
                invocation -> Optional.ofNullable(deliveries.get(invocation.getArgument(0))));
        when(repository.findByEventIdAndStoreNumber(any(), anyInt())).thenAnswer(invocation -> deliveries.values()
                .stream().filter(delivery -> delivery.getEventId().equals(invocation.getArgument(0))
                        && delivery.getStoreNumber() == (int) invocation.getArgument(1)).findFirst());
    }

    @Test
    void recordsAcknowledgementForAnyOfTheThirtyTwoStores() {
        when(gateway.deliver(anyInt(), any(), anyString(), any(), anyString(), any())).thenReturn(
                new PosGatewayPort.DeliveryResult(true, "POS-32-ACK", null));

        PosDelivery delivery = service.publish(UUID.randomUUID(), "OrderConfirmed", UUID.randomUUID(),
                32, "{\"status\":\"CONFIRMED\"}", UUID.randomUUID());

        assertThat(delivery.getStatus()).isEqualTo(PosDeliveryStatus.ACKNOWLEDGED);
        assertThat(delivery.getAcknowledgement()).isEqualTo("POS-32-ACK");
        assertThat(delivery.getAttempts()).isEqualTo(1);
    }

    @Test
    void duplicateEventForSameStoreReturnsExistingDeliveryWithoutResending() {
        when(gateway.deliver(anyInt(), any(), anyString(), any(), anyString(), any())).thenReturn(
                new PosGatewayPort.DeliveryResult(true, "POS-ACK", null));
        UUID eventId = UUID.randomUUID();

        PosDelivery first = service.publish(eventId, "OrderConfirmed", UUID.randomUUID(), 7,
                "{}", UUID.randomUUID());
        PosDelivery repeated = service.publish(eventId, "OrderConfirmed", UUID.randomUUID(), 7,
                "{}", UUID.randomUUID());

        assertThat(repeated).isSameAs(first);
        verify(gateway, times(1)).deliver(anyInt(), any(), anyString(), any(), anyString(), any());
    }

    @Test
    void transientPosFailureRetriesThreeTimesAndMovesDeliveryToDeadLetter() {
        when(gateway.deliver(anyInt(), any(), anyString(), any(), anyString(), any()))
                .thenThrow(new PosGatewayPort.PosUnavailableException("offline"));

        PosDelivery delivery = service.publish(UUID.randomUUID(), "OrderConfirmed", UUID.randomUUID(),
                1, "{}", UUID.randomUUID());

        assertThat(delivery.getStatus()).isEqualTo(PosDeliveryStatus.DEAD_LETTER);
        assertThat(delivery.getAttempts()).isEqualTo(4);
        verify(gateway, times(4)).deliver(anyInt(), any(), anyString(), any(), anyString(), any());
    }
}
