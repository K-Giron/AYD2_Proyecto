package com.megastore.sgologic.logistics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * El evento de tracking no debe responder 500 cuando coincide con la
 * actualización de la guía que hace otro hilo sobre el mismo despacho.
 */
class ShipmentControllerTrackingRetryTest {

    private final UUID shipmentId = UUID.randomUUID();
    private ShipmentService service;
    private ShipmentController controller;
    private Shipment shipment;

    @BeforeEach
    void setUp() {
        service = mock(ShipmentService.class);
        controller = new ShipmentController(service);
        shipment = Shipment.assign(UUID.randomUUID(), DispatchOrigin.DISTRIBUTION_CENTER, "Zona 1",
                UUID.randomUUID(), "key-" + UUID.randomUUID(), Instant.now());
    }

    @Test
    void aConcurrentUpdateIsRetriedAndTheEventIsRecorded() {
        when(service.recordTrackingEvent(eq(shipmentId), eq(ShipmentStatus.IN_TRANSIT), any()))
                .thenThrow(conflict())
                .thenThrow(conflict())
                .thenReturn(shipment);

        ShipmentResponse response = controller.recordTrackingEvent(shipmentId,
                new RecordTrackingEventRequest(ShipmentStatus.IN_TRANSIT));

        assertThat(response).isNotNull();
        verify(service, times(3)).recordTrackingEvent(eq(shipmentId), eq(ShipmentStatus.IN_TRANSIT), any());
    }

    @Test
    void aPersistentConflictIsEventuallyReportedInsteadOfLoopingForever() {
        when(service.recordTrackingEvent(eq(shipmentId), eq(ShipmentStatus.IN_TRANSIT), any()))
                .thenThrow(conflict());

        assertThatThrownBy(() -> controller.recordTrackingEvent(shipmentId,
                new RecordTrackingEventRequest(ShipmentStatus.IN_TRANSIT)))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        verify(service, times(3)).recordTrackingEvent(eq(shipmentId), eq(ShipmentStatus.IN_TRANSIT), any());
    }

    @Test
    void otherFailuresAreNotRetried() {
        when(service.recordTrackingEvent(eq(shipmentId), eq(ShipmentStatus.IN_TRANSIT), any()))
                .thenThrow(new ShipmentNotFoundException(shipmentId));

        assertThatThrownBy(() -> controller.recordTrackingEvent(shipmentId,
                new RecordTrackingEventRequest(ShipmentStatus.IN_TRANSIT)))
                .isInstanceOf(ShipmentNotFoundException.class);
        verify(service, times(1)).recordTrackingEvent(eq(shipmentId), eq(ShipmentStatus.IN_TRANSIT), any());
    }

    private ObjectOptimisticLockingFailureException conflict() {
        return new ObjectOptimisticLockingFailureException(Shipment.class, shipmentId);
    }
}
