package com.megastore.inventoryservice.posstock;

import com.megastore.inventoryservice.posevents.PosStockEventService;
import com.megastore.inventoryservice.auditlog.AuditLogService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PosStockServiceTest {

    private final PosStockRepository stockRepository = mock(PosStockRepository.class);
    private final PosStockMovementRepository movementRepository = mock(PosStockMovementRepository.class);
    private final PosFolioSequenceRepository folioSequenceRepository = mock(PosFolioSequenceRepository.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final PosStockEventService stockEvents = mock(PosStockEventService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-02T10:00:00Z"), ZoneOffset.UTC);
    private final PosStockService service = new PosStockService(
            stockRepository, movementRepository, folioSequenceRepository, auditLogService, stockEvents, clock);

    @Test
    void receiveFromWarehouseCreatesStockRowWhenFirstTimeForThatArticle() {
        UUID pointOfSaleId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        when(stockRepository.findByPointOfSaleIdAndArticleId(pointOfSaleId, articleId)).thenReturn(Optional.empty());
        when(folioSequenceRepository.nextEntradaSequence()).thenReturn(1L);
        when(movementRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PosStockMovement movement = service.receiveFromWarehouse(pointOfSaleId, articleId, 10, "SAL-000001");

        assertThat(movement.getFolio()).isEqualTo("POS-ENT-000001");
        assertThat(movement.getQuantity()).isEqualTo(10);
        assertThat(movement.getOriginWarehouseFolio()).isEqualTo("SAL-000001");
        verify(stockRepository).saveAndFlush(any());
        verify(stockRepository).increase(pointOfSaleId, articleId, 10, clock.instant());
        verify(auditLogService).record(eq("POS_ENTRADA_REGISTRADA"), anyString(), eq("SAL-000001"), eq(pointOfSaleId));
        // El POS de esa tienda recibe el evento de stock (bandeja de salida, misma transacción).
        verify(stockEvents).enqueue(eq(pointOfSaleId), eq(articleId), eq("ENTRADA"), eq(10),
                org.mockito.ArgumentMatchers.anyInt(), eq("POS-ENT-000001"), eq("SAL-000001"));
    }

    @Test
    void receiveFromWarehouseReusesExistingStockRowOnSubsequentDeliveries() {
        UUID pointOfSaleId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        PosStock existing = PosStock.initialize(pointOfSaleId, articleId, clock.instant());
        when(stockRepository.findByPointOfSaleIdAndArticleId(pointOfSaleId, articleId)).thenReturn(Optional.of(existing));
        when(folioSequenceRepository.nextEntradaSequence()).thenReturn(2L);
        when(movementRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PosStockMovement movement = service.receiveFromWarehouse(pointOfSaleId, articleId, 5, "SAL-000002");

        assertThat(movement.getFolio()).isEqualTo("POS-ENT-000002");
        verify(stockRepository, never()).saveAndFlush(any());
        verify(stockRepository).increase(pointOfSaleId, articleId, 5, clock.instant());
    }

    @Test
    void registerSaleDecreasesPointOfSaleStockWithItsOwnFolio() {
        UUID pointOfSaleId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        when(stockRepository.decrease(pointOfSaleId, articleId, 2, clock.instant())).thenReturn(1);
        when(folioSequenceRepository.nextVentaSequence()).thenReturn(7L);
        when(movementRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PosStockMovement movement = service.registerSale(pointOfSaleId, articleId, 2, "Venta en tienda", "STORE:o:p");

        assertThat(movement.getFolio()).isEqualTo("POS-VTA-000007");
        assertThat(movement.getType()).isEqualTo(PosStockMovementType.VENTA);
        assertThat(movement.getExternalReference()).isEqualTo("STORE:o:p");
        verify(auditLogService).record(eq("POS_VENTA_REGISTRADA"), anyString(), eq("POS-VTA-000007"), eq(pointOfSaleId));
    }

    @Test
    void registerSaleIsIdempotentByExternalReference() {
        UUID pointOfSaleId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        PosStockMovement existing = PosStockMovement.sale("POS-VTA-000001", pointOfSaleId, articleId, 1,
                "STORE:o:p", clock.instant());
        when(movementRepository.findByExternalReference("STORE:o:p")).thenReturn(Optional.of(existing));

        PosStockMovement movement = service.registerSale(pointOfSaleId, articleId, 1, null, "STORE:o:p");

        assertThat(movement).isSameAs(existing);
        verify(stockRepository, never()).decrease(any(), any(), anyInt(), any());
    }

    @Test
    void registerSaleRejectsWhenPointOfSaleHasNotEnoughStock() {
        UUID pointOfSaleId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        when(stockRepository.decrease(pointOfSaleId, articleId, 9, clock.instant())).thenReturn(0);

        assertThatThrownBy(() -> service.registerSale(pointOfSaleId, articleId, 9, null, null))
                .isInstanceOf(InsufficientPosStockException.class);
        verify(movementRepository, never()).saveAndFlush(any());
    }
}
