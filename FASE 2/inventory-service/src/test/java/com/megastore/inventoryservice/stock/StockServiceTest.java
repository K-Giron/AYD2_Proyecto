package com.megastore.inventoryservice.stock;

import com.megastore.inventoryservice.article.ArticleNotFoundException;
import com.megastore.inventoryservice.article.ArticleRepository;
import com.megastore.inventoryservice.auditlog.AuditLogService;
import com.megastore.inventoryservice.logistics.ShipmentService;
import com.megastore.inventoryservice.pointofsale.PointOfSaleResponse;
import com.megastore.inventoryservice.pointofsale.PointOfSaleService;
import com.megastore.inventoryservice.posstock.PosStockService;
import com.megastore.inventoryservice.warehouse.Warehouse;
import com.megastore.inventoryservice.warehouse.WarehouseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StockServiceTest {

    private final ArticleRepository articleRepository = mock(ArticleRepository.class);
    private final StockRepository stockRepository = mock(StockRepository.class);
    private final StockMovementRepository movementRepository = mock(StockMovementRepository.class);
    private final FolioGenerator folioGenerator = mock(FolioGenerator.class);
    private final ShipmentService shipmentService = mock(ShipmentService.class);
    private final PointOfSaleService pointOfSaleService = mock(PointOfSaleService.class);
    private final PosStockService posStockService = mock(PosStockService.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-01T10:00:00Z"), ZoneOffset.UTC);
    private final UUID centralWarehouseId = UUID.randomUUID();
    private final StockService service = new StockService(
            articleRepository, stockRepository, movementRepository, folioGenerator, shipmentService,
            pointOfSaleService, posStockService, auditLogService, warehouseRepository, clock);

    {
        when(warehouseRepository.findByCode(Warehouse.CENTRAL_CODE))
                .thenReturn(Optional.of(centralWarehouse(centralWarehouseId)));
        when(warehouseRepository.findById(centralWarehouseId))
                .thenReturn(Optional.of(centralWarehouse(centralWarehouseId)));
    }

    private static Warehouse centralWarehouse(UUID id) {
        try {
            var constructor = Warehouse.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Warehouse warehouse = constructor.newInstance();
            ReflectionTestUtils.setField(warehouse, "id", id);
            ReflectionTestUtils.setField(warehouse, "code", Warehouse.CENTRAL_CODE);
            ReflectionTestUtils.setField(warehouse, "name", "Almacén Central");
            return warehouse;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static PointOfSaleResponse pointOfSale(UUID id) {
        return new PointOfSaleResponse(id, "POS-01", "Tienda Centro",
                UUID.randomUUID(), "ALM-POS-01", "Almacén Tienda Centro");
    }

    @Test
    void adjustmentDecreasesStockWithItsOwnFolioAndNoPointOfSale() {
        UUID articleId = UUID.randomUUID();
        when(articleRepository.existsById(articleId)).thenReturn(true);
        when(stockRepository.decrease(articleId, 3, clock.instant())).thenReturn(1);
        when(folioGenerator.next(StockMovementType.AJUSTE)).thenReturn("AJU-000001");
        when(movementRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        StockMovement movement = service.registerMovement(articleId, StockMovementType.AJUSTE, 3,
                "Merma por daño", UUID.randomUUID(), null);

        assertThat(movement.getFolio()).isEqualTo("AJU-000001");
        assertThat(movement.getPointOfSaleId()).isNull();
        verify(shipmentService, never()).createForFolio(any(), any(), any());
        verify(posStockService, never()).receiveFromWarehouse(any(), any(), anyInt(), any());
        verify(auditLogService).record(org.mockito.ArgumentMatchers.eq("AJUSTE_REGISTRADO"),
                org.mockito.ArgumentMatchers.contains("Merma por daño"), org.mockito.ArgumentMatchers.eq("AJU-000001"),
                org.mockito.ArgumentMatchers.isNull());
    }

    @Test
    void adjustmentRequiresAReason() {
        UUID articleId = UUID.randomUUID();
        when(articleRepository.existsById(articleId)).thenReturn(true);

        assertThatThrownBy(() -> service.registerMovement(articleId, StockMovementType.AJUSTE, 1, " ", null, null))
                .isInstanceOf(AdjustmentReasonRequiredException.class);
        verify(stockRepository, never()).decrease(any(), anyInt(), any());
    }

    @Test
    void registerMovementIncreasesStockAndAssignsEntradaFolio() {
        UUID articleId = UUID.randomUUID();
        when(articleRepository.existsById(articleId)).thenReturn(true);
        when(stockRepository.increase(articleId, 10, clock.instant())).thenReturn(1);
        when(folioGenerator.next(StockMovementType.ENTRADA)).thenReturn("ENT-000001");
        when(movementRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        StockMovement movement = service.registerMovement(articleId, StockMovementType.ENTRADA, 10, "Reposición", null, null);

        assertThat(movement.getFolio()).isEqualTo("ENT-000001");
        assertThat(movement.getType()).isEqualTo(StockMovementType.ENTRADA);
        assertThat(movement.getQuantity()).isEqualTo(10);
        assertThat(movement.getPointOfSaleId()).isNull();
        assertThat(movement.getWarehouseId()).isEqualTo(centralWarehouseId);
        verify(stockRepository, never()).decrease(any(), anyInt(), any());
        verify(shipmentService, never()).createForFolio(any(), any(), any());
    }

    @Test
    void registerMovementDecreasesStockAndAssignsSalidaFolio() {
        UUID articleId = UUID.randomUUID();
        UUID pointOfSaleId = UUID.randomUUID();
        when(articleRepository.existsById(articleId)).thenReturn(true);
        when(pointOfSaleService.getResponseById(pointOfSaleId)).thenReturn(pointOfSale(pointOfSaleId));
        when(stockRepository.decrease(articleId, 4, clock.instant())).thenReturn(1);
        when(folioGenerator.next(StockMovementType.SALIDA)).thenReturn("SAL-000001");
        when(movementRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        StockMovement movement = service.registerMovement(articleId, StockMovementType.SALIDA, 4, "Envío a tienda", pointOfSaleId, null);

        assertThat(movement.getFolio()).isEqualTo("SAL-000001");
        assertThat(movement.getPointOfSaleId()).isEqualTo(pointOfSaleId);
        assertThat(movement.getWarehouseId()).isEqualTo(centralWarehouseId);
        verify(shipmentService).createForFolio("SAL-000001", movement.getId(), pointOfSaleId);
        verify(posStockService).receiveFromWarehouse(pointOfSaleId, articleId, 4, "SAL-000001");
    }

    @Test
    void registerMovementReturnsExistingMovementWhenExternalReferenceAlreadyRegistered() {
        UUID articleId = UUID.randomUUID();
        UUID pointOfSaleId = UUID.randomUUID();
        String externalReference = "WEB:" + UUID.randomUUID() + ":" + articleId;
        StockMovement existing = StockMovement.register("SAL-000002", StockMovementType.SALIDA, articleId,
                centralWarehouseId, 3, "Venta web", pointOfSaleId, externalReference, clock.instant());
        when(movementRepository.findByExternalReference(externalReference)).thenReturn(Optional.of(existing));

        StockMovement movement = service.registerMovement(articleId, StockMovementType.SALIDA, 3, "Venta web",
                pointOfSaleId, externalReference);

        assertThat(movement).isSameAs(existing);
        verify(articleRepository, never()).existsById(any());
        verify(stockRepository, never()).decrease(any(), anyInt(), any());
        verify(movementRepository, never()).saveAndFlush(any());
        verify(shipmentService, never()).createForFolio(any(), any(), any());
        verify(posStockService, never()).receiveFromWarehouse(any(), any(), anyInt(), any());
    }

    @Test
    void registerMovementThrowsPointOfSaleRequiredWhenSalidaHasNoDestination() {
        UUID articleId = UUID.randomUUID();
        when(articleRepository.existsById(articleId)).thenReturn(true);

        assertThatThrownBy(() -> service.registerMovement(articleId, StockMovementType.SALIDA, 1, null, null, null))
                .isInstanceOf(PointOfSaleRequiredException.class);

        verify(stockRepository, never()).decrease(any(), anyInt(), any());
        verify(movementRepository, never()).saveAndFlush(any());
    }

    @Test
    void registerMovementThrowsInsufficientStockWhenSalidaExceedsAvailability() {
        UUID articleId = UUID.randomUUID();
        UUID pointOfSaleId = UUID.randomUUID();
        when(articleRepository.existsById(articleId)).thenReturn(true);
        when(pointOfSaleService.getResponseById(pointOfSaleId)).thenReturn(pointOfSale(pointOfSaleId));
        when(stockRepository.decrease(articleId, 999, clock.instant())).thenReturn(0);

        assertThatThrownBy(() -> service.registerMovement(articleId, StockMovementType.SALIDA, 999, null, pointOfSaleId, null))
                .isInstanceOf(InsufficientStockException.class);

        verify(movementRepository, never()).saveAndFlush(any());
    }

    @Test
    void registerMovementThrowsArticleNotFoundWhenArticleDoesNotExist() {
        UUID articleId = UUID.randomUUID();
        when(articleRepository.existsById(articleId)).thenReturn(false);

        assertThatThrownBy(() -> service.registerMovement(articleId, StockMovementType.ENTRADA, 1, null, null, null))
                .isInstanceOf(ArticleNotFoundException.class);

        verify(stockRepository, never()).increase(any(), anyInt(), any());
    }
}
