package com.megastore.inventoryservice.storesync;

import com.megastore.inventoryservice.article.ArticleService;
import com.megastore.inventoryservice.auditlog.AuditLogService;
import com.megastore.inventoryservice.pointofsale.PointOfSaleResponse;
import com.megastore.inventoryservice.pointofsale.PointOfSaleService;
import com.megastore.inventoryservice.posstock.InsufficientPosStockException;
import com.megastore.inventoryservice.posstock.PosStockMovement;
import com.megastore.inventoryservice.posstock.PosStockService;
import com.megastore.inventoryservice.stock.StockMovement;
import com.megastore.inventoryservice.stock.StockMovementType;
import com.megastore.inventoryservice.stock.StockService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
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

class StoreSyncServiceTest {

    private final PointOfSaleService pointOfSaleService = mock(PointOfSaleService.class);
    private final PosStockService posStockService = mock(PosStockService.class);
    private final StockService stockService = mock(StockService.class);
    private final ArticleService articleService = mock(ArticleService.class);
    private final StoreInvoiceRepository invoiceRepository = mock(StoreInvoiceRepository.class);
    private final StoreTransferRepository transferRepository = mock(StoreTransferRepository.class);
    private final StoreFolioSequenceRepository folioSequenceRepository = mock(StoreFolioSequenceRepository.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-05T10:00:00Z"), ZoneOffset.UTC);
    private final StoreSyncService service = new StoreSyncService(pointOfSaleService, posStockService, stockService,
            articleService, invoiceRepository, transferRepository, folioSequenceRepository, auditLogService, clock);

    private final PointOfSaleResponse pos01 = new PointOfSaleResponse(UUID.randomUUID(), "POS-01", "Tienda 1",
            UUID.randomUUID(), "ALM-POS-01", "Almacén Tienda 1");
    private final PointOfSaleResponse pos02 = new PointOfSaleResponse(UUID.randomUUID(), "POS-02", "Tienda 2",
            UUID.randomUUID(), "ALM-POS-02", "Almacén Tienda 2");
    private final UUID articleId = UUID.randomUUID();

    private UploadStoreInvoiceRequest invoice(String ticket) {
        return new UploadStoreInvoiceRequest(ticket, clock.instant(), "maria", "CF", "Consumidor Final", "EFECTIVO",
                new BigDecimal("200.00"), List.of(new UploadStoreInvoiceRequest.Line(articleId, "MS-CAM-101",
                "Camisa", new BigDecimal("100.00"), 2, new BigDecimal("200.00"))));
    }

    @Test
    void uploadInvoiceStoresItWithCentralFolioAndDecreasesStoreStock() {
        when(invoiceRepository.findByPointOfSaleCodeAndTicketFolio("POS-01", "TKT-000001")).thenReturn(Optional.empty());
        when(pointOfSaleService.getResponseByCode("POS-01")).thenReturn(pos01);
        when(folioSequenceRepository.nextInvoiceSequence()).thenReturn(3L);
        when(invoiceRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(posStockService.tryRegisterSale(eq(pos01.id()), eq(articleId), eq(2), anyString(), anyString()))
                .thenReturn(Optional.of(mock(PosStockMovement.class)));

        StoreInvoice stored = service.uploadInvoice("POS-01", invoice("TKT-000001"));

        assertThat(stored.getCentralFolio()).isEqualTo("FT-000003");
        assertThat(stored.getTaxId()).isEqualTo("CF");
        assertThat(stored.getLines()).hasSize(1);
        verify(posStockService).tryRegisterSale(pos01.id(), articleId, 2, "Ticket TKT-000001",
                "TKT:POS-01:TKT-000001:" + articleId);
        verify(auditLogService, never()).record(eq("DISCREPANCIA_STOCK_TIENDA"), anyString(), any(), any());
    }

    @Test
    void uploadInvoiceIsIdempotentByTicketFolio() {
        StoreInvoice existing = StoreInvoice.receive("FT-000001", pos01.id(), "POS-01", invoice("TKT-000001"),
                clock.instant());
        when(invoiceRepository.findByPointOfSaleCodeAndTicketFolio("POS-01", "TKT-000001"))
                .thenReturn(Optional.of(existing));

        StoreInvoice stored = service.uploadInvoice("POS-01", invoice("TKT-000001"));

        assertThat(stored).isSameAs(existing);
        verify(posStockService, never()).tryRegisterSale(any(), any(), anyInt(), any(), any());
        verify(invoiceRepository, never()).saveAndFlush(any());
    }

    @Test
    void uploadInvoiceIsKeptWhenCentralViewOfStoreStockDoesNotMatch() {
        when(invoiceRepository.findByPointOfSaleCodeAndTicketFolio("POS-01", "TKT-000002")).thenReturn(Optional.empty());
        when(pointOfSaleService.getResponseByCode("POS-01")).thenReturn(pos01);
        when(folioSequenceRepository.nextInvoiceSequence()).thenReturn(4L);
        when(invoiceRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(posStockService.tryRegisterSale(any(), any(), anyInt(), any(), any())).thenReturn(Optional.empty());

        StoreInvoice stored = service.uploadInvoice("POS-01", invoice("TKT-000002"));

        assertThat(stored.getCentralFolio()).isEqualTo("FT-000004");
        verify(auditLogService).record(eq("DISCREPANCIA_STOCK_TIENDA"), anyString(), eq("FT-000004"), eq(pos01.id()));
    }

    @Test
    void transferToCentralDecreasesStoreAndEntersCentralStock() {
        RegisterStoreTransferRequest request = new RegisterStoreTransferRequest("TRS-000001", articleId, 3,
                "CENTRAL", "Sobrante", "maria");
        when(transferRepository.findByOriginPosCodeAndLocalFolio("POS-01", "TRS-000001")).thenReturn(Optional.empty());
        when(pointOfSaleService.getResponseByCode("POS-01")).thenReturn(pos01);
        when(folioSequenceRepository.nextTransferSequence()).thenReturn(1L);
        StockMovement entry = mock(StockMovement.class);
        when(entry.getFolio()).thenReturn("ENT-000009");
        when(stockService.registerMovement(eq(articleId), eq(StockMovementType.ENTRADA), eq(3), anyString(),
                eq(pos01.id()), eq("TRA:TRA-000001"))).thenReturn(entry);
        when(transferRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        StoreTransfer transfer = service.registerTransfer("POS-01", request);

        assertThat(transfer.getCentralFolio()).isEqualTo("TRA-000001");
        assertThat(transfer.getDestinationFolio()).isEqualTo("ENT-000009");
        verify(posStockService).transferOut(pos01.id(), articleId, 3, "TRA-000001", "TRS:POS-01:TRS-000001");
        verify(posStockService, never()).receiveFromWarehouse(any(), any(), anyInt(), any());
    }

    @Test
    void transferToAnotherStoreCreatesItsReceipt() {
        RegisterStoreTransferRequest request = new RegisterStoreTransferRequest("TRS-000002", articleId, 1,
                "POS-02", null, "maria");
        when(transferRepository.findByOriginPosCodeAndLocalFolio("POS-01", "TRS-000002")).thenReturn(Optional.empty());
        when(pointOfSaleService.getResponseByCode("POS-01")).thenReturn(pos01);
        when(pointOfSaleService.getResponseByCode("POS-02")).thenReturn(pos02);
        when(folioSequenceRepository.nextTransferSequence()).thenReturn(2L);
        PosStockMovement receipt = mock(PosStockMovement.class);
        when(receipt.getFolio()).thenReturn("POS-ENT-000005");
        when(posStockService.receiveFromWarehouse(pos02.id(), articleId, 1, "TRA-000002")).thenReturn(receipt);
        when(transferRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        StoreTransfer transfer = service.registerTransfer("POS-01", request);

        assertThat(transfer.getDestinationFolio()).isEqualTo("POS-ENT-000005");
        verify(stockService, never()).registerMovement(any(), any(), anyInt(), any(), any(), any());
    }

    @Test
    void transferWithoutStoreStockRegistersNothing() {
        RegisterStoreTransferRequest request = new RegisterStoreTransferRequest("TRS-000003", articleId, 50,
                "CENTRAL", null, "maria");
        when(transferRepository.findByOriginPosCodeAndLocalFolio("POS-01", "TRS-000003")).thenReturn(Optional.empty());
        when(pointOfSaleService.getResponseByCode("POS-01")).thenReturn(pos01);
        when(folioSequenceRepository.nextTransferSequence()).thenReturn(3L);
        when(posStockService.transferOut(any(), any(), anyInt(), any(), any()))
                .thenThrow(new InsufficientPosStockException(pos01.id(), articleId, 50));

        assertThatThrownBy(() -> service.registerTransfer("POS-01", request))
                .isInstanceOf(InsufficientPosStockException.class);
        verify(stockService, never()).registerMovement(any(), any(), anyInt(), any(), any(), any());
        verify(transferRepository, never()).saveAndFlush(any());
    }

    @Test
    void transferToItselfIsRejected() {
        RegisterStoreTransferRequest request = new RegisterStoreTransferRequest("TRS-000004", articleId, 1,
                "POS-01", null, "maria");
        when(transferRepository.findByOriginPosCodeAndLocalFolio("POS-01", "TRS-000004")).thenReturn(Optional.empty());
        when(pointOfSaleService.getResponseByCode("POS-01")).thenReturn(pos01);

        assertThatThrownBy(() -> service.registerTransfer("POS-01", request))
                .isInstanceOf(InvalidStoreTransferException.class);
    }
}
