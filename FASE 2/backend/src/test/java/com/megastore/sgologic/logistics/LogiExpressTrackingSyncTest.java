package com.megastore.sgologic.logistics;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LogiExpressTrackingSyncTest {

    private final LogiExpressTrackingPort port = mock(LogiExpressTrackingPort.class);
    private final ShipmentService shipments = mock(ShipmentService.class);
    private final LogiExpressTrackingSync sync = new LogiExpressTrackingSync(port, shipments);
    private final UUID shipmentId = UUID.randomUUID();

    private void shipmentIn(ShipmentStatus status) {
        Shipment shipment = mock(Shipment.class);
        when(shipment.getStatus()).thenReturn(status);
        when(shipments.getShipment(shipmentId)).thenReturn(shipment);
    }

    private void carrierReports(String status) throws Exception {
        when(port.guidesUpdatedSince(any())).thenReturn(List.of(new LogiExpressTrackingPort.TrackedGuide(
                "LGX-00000003", shipmentId.toString(), status, Instant.parse("2026-10-06T20:00:00Z"))));
    }

    @Test
    void deliveryReportedByTheCarrierCompletesTheShipmentHistory() throws Exception {
        shipmentIn(ShipmentStatus.LABEL_ISSUED);
        carrierReports("ENTREGADA");

        sync.poll();

        InOrder order = inOrder(shipments);
        order.verify(shipments).recordTrackingEvent(shipmentId, ShipmentStatus.IN_TRANSIT, "LOGIEXPRESS");
        order.verify(shipments).recordTrackingEvent(shipmentId, ShipmentStatus.DELIVERED, "LOGIEXPRESS");
    }

    @Test
    void parcelLeftAtTheStoreIsReadyForPickup() throws Exception {
        shipmentIn(ShipmentStatus.IN_TRANSIT);
        carrierReports("EN_PUNTO_DE_RETIRO");

        sync.poll();

        verify(shipments).recordTrackingEvent(shipmentId, ShipmentStatus.READY_FOR_PICKUP, "LOGIEXPRESS");
        verify(shipments, never()).recordTrackingEvent(shipmentId, ShipmentStatus.IN_TRANSIT, "LOGIEXPRESS");
    }

    @Test
    void trackingNeverMovesAShipmentBackwards() throws Exception {
        shipmentIn(ShipmentStatus.DELIVERED);
        carrierReports("EN_TRANSITO");

        sync.poll();

        verify(shipments, never()).recordTrackingEvent(any(), any(), anyString());
        verify(shipments).reconcileLabel(shipmentId, "LGX-00000003");
    }

    @Test
    void carrierOutageIsRetriedOnTheNextCycle() throws Exception {
        when(port.guidesUpdatedSince(any())).thenThrow(new LogiExpressUnavailableException("503"));

        sync.poll();

        verify(shipments, never()).getShipment(any());
    }
}
