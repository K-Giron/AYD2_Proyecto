package com.megastore.inventoryservice.logistics;

import com.megastore.inventoryservice.auditlog.AuditLogService;
import com.megastore.inventoryservice.pointofsale.PointOfSaleService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShipmentServiceTest {

    private final ShipmentRepository repository = mock(ShipmentRepository.class);
    private final PointOfSaleService pointOfSaleService = mock(PointOfSaleService.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-01T10:00:00Z"), ZoneOffset.UTC);
    private final ShipmentService service = new ShipmentService(repository, pointOfSaleService, auditLogService, clock);

    @Test
    void createForFolioStartsInPendiente() {
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        UUID pointOfSaleId = UUID.randomUUID();

        Shipment shipment = service.createForFolio("SAL-000001", UUID.randomUUID(), pointOfSaleId);

        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.PENDIENTE);
        assertThat(shipment.getFolio()).isEqualTo("SAL-000001");
        assertThat(shipment.getPointOfSaleId()).isEqualTo(pointOfSaleId);
    }

    @Test
    void advanceStatusMovesForwardOneStepAtATime() {
        Shipment shipment = Shipment.createForFolio("SAL-000001", UUID.randomUUID(), UUID.randomUUID(), clock.instant());
        when(repository.findByFolio("SAL-000001")).thenReturn(Optional.of(shipment));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Shipment advanced = service.advanceStatus("SAL-000001", ShipmentStatus.EN_TRANSITO);

        assertThat(advanced.getStatus()).isEqualTo(ShipmentStatus.EN_TRANSITO);
    }

    @Test
    void advanceStatusRejectsSkippingAStep() {
        Shipment shipment = Shipment.createForFolio("SAL-000002", UUID.randomUUID(), UUID.randomUUID(), clock.instant());
        when(repository.findByFolio("SAL-000002")).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> service.advanceStatus("SAL-000002", ShipmentStatus.ENTREGADO))
                .isInstanceOf(InvalidShipmentTransitionException.class);
    }

    @Test
    void getByFolioThrowsWhenShipmentDoesNotExist() {
        when(repository.findByFolio("SAL-999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByFolio("SAL-999999")).isInstanceOf(ShipmentNotFoundException.class);
    }
}
