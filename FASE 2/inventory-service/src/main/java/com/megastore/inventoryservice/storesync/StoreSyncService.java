package com.megastore.inventoryservice.storesync;

import com.megastore.inventoryservice.article.Article;
import com.megastore.inventoryservice.article.ArticleService;
import com.megastore.inventoryservice.auditlog.AuditLogService;
import com.megastore.inventoryservice.pointofsale.PointOfSaleResponse;
import com.megastore.inventoryservice.pointofsale.PointOfSaleService;
import com.megastore.inventoryservice.posstock.PosStockMovement;
import com.megastore.inventoryservice.posstock.PosStockService;
import com.megastore.inventoryservice.stock.StockMovement;
import com.megastore.inventoryservice.stock.StockMovementType;
import com.megastore.inventoryservice.stock.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Lado central de la integración con cada sistema de tienda: la tienda
 * baja sus recepciones (folios POS-ENT), sube las facturas de lo que
 * vendió y registra traslados por folio. Todo es idempotente por el folio
 * local de la tienda, porque la tienda reintenta cuando no hay conexión.
 */
@Service
public class StoreSyncService {

    private final PointOfSaleService pointOfSaleService;
    private final PosStockService posStockService;
    private final StockService stockService;
    private final ArticleService articleService;
    private final StoreInvoiceRepository invoiceRepository;
    private final StoreTransferRepository transferRepository;
    private final StoreFolioSequenceRepository folioSequenceRepository;
    private final AuditLogService auditLogService;
    private final Clock clock;

    public StoreSyncService(PointOfSaleService pointOfSaleService, PosStockService posStockService,
                            StockService stockService, ArticleService articleService,
                            StoreInvoiceRepository invoiceRepository, StoreTransferRepository transferRepository,
                            StoreFolioSequenceRepository folioSequenceRepository, AuditLogService auditLogService,
                            Clock clock) {
        this.pointOfSaleService = pointOfSaleService;
        this.posStockService = posStockService;
        this.stockService = stockService;
        this.articleService = articleService;
        this.invoiceRepository = invoiceRepository;
        this.transferRepository = transferRepository;
        this.folioSequenceRepository = folioSequenceRepository;
        this.auditLogService = auditLogService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<StoreReceiptResponse> receipts(String posCode, Instant since) {
        PointOfSaleResponse pointOfSale = pointOfSaleService.getResponseByCode(posCode);
        return posStockService.listReceiptsSince(pointOfSale.id(), since == null ? Instant.EPOCH : since).stream()
                .map(movement -> {
                    Article article = articleService.getById(movement.getArticleId());
                    return new StoreReceiptResponse(movement.getFolio(), movement.getOriginWarehouseFolio(),
                            originOf(movement.getOriginWarehouseFolio()), article.getId(), article.getSku(),
                            article.getName(), article.getDescription(), article.getPrice(), movement.getQuantity(),
                            movement.getOccurredAt());
                })
                .toList();
    }

    /**
     * Recibe la factura de una venta ya hecha en la tienda. La venta ya
     * ocurrió (la tienda vende desde su propio stock aunque el central esté
     * caído), así que nunca se rechaza por existencia: si la vista central
     * del stock de la tienda no alcanza, se registra la factura igual y se
     * deja la discrepancia en la bitácora.
     */
    @Transactional
    public StoreInvoice uploadInvoice(String posCode, UploadStoreInvoiceRequest request) {
        Optional<StoreInvoice> existing = invoiceRepository.findByPointOfSaleCodeAndTicketFolio(posCode, request.ticketFolio());
        if (existing.isPresent()) {
            return existing.get();
        }
        PointOfSaleResponse pointOfSale = pointOfSaleService.getResponseByCode(posCode);
        String centralFolio = "FT-" + String.format("%06d", folioSequenceRepository.nextInvoiceSequence());
        StoreInvoice invoice = invoiceRepository.saveAndFlush(
                StoreInvoice.receive(centralFolio, pointOfSale.id(), posCode, request, clock.instant()));
        for (UploadStoreInvoiceRequest.Line line : request.lines()) {
            boolean registered = posStockService.tryRegisterSale(pointOfSale.id(), line.articleId(), line.quantity(),
                    "Ticket " + request.ticketFolio(),
                    "TKT:" + posCode + ":" + request.ticketFolio() + ":" + line.articleId()).isPresent();
            if (!registered) {
                auditLogService.record("DISCREPANCIA_STOCK_TIENDA",
                        "La tienda " + posCode + " vendió " + line.quantity() + " de " + line.sku()
                                + " (ticket " + request.ticketFolio() + ") pero la vista central de su stock no alcanza.",
                        centralFolio, pointOfSale.id());
            }
        }
        auditLogService.record("FACTURA_TIENDA_RECIBIDA",
                "La tienda " + posCode + " subió el ticket " + request.ticketFolio() + " por Q" + request.total()
                        + " (" + request.taxId() + ", " + request.customerName() + "), registrado como " + centralFolio + ".",
                centralFolio, pointOfSale.id());
        return invoice;
    }

    /**
     * Traslado por folio desde una tienda: baja el stock de la tienda y lo
     * suma al almacén central (entrada ENT) o a otra tienda (entrada
     * POS-ENT, que esa tienda baja en su próxima sincronización).
     */
    @Transactional
    public StoreTransfer registerTransfer(String posCode, RegisterStoreTransferRequest request) {
        Optional<StoreTransfer> existing = transferRepository.findByOriginPosCodeAndLocalFolio(posCode, request.localFolio());
        if (existing.isPresent()) {
            return existing.get();
        }
        PointOfSaleResponse origin = pointOfSaleService.getResponseByCode(posCode);
        if (posCode.equals(request.destination())) {
            throw new InvalidStoreTransferException("Una tienda no puede trasladarse mercadería a sí misma.");
        }
        PointOfSaleResponse destinationStore = StoreTransfer.CENTRAL.equals(request.destination())
                ? null
                : pointOfSaleService.getResponseByCode(request.destination());
        String centralFolio = "TRA-" + String.format("%06d", folioSequenceRepository.nextTransferSequence());

        posStockService.transferOut(origin.id(), request.articleId(), request.quantity(), centralFolio,
                "TRS:" + posCode + ":" + request.localFolio());

        String destinationFolio;
        if (destinationStore == null) {
            StockMovement entry = stockService.registerMovement(request.articleId(), StockMovementType.ENTRADA,
                    request.quantity(), "Traslado desde tienda " + posCode + " (" + centralFolio + ")"
                            + (request.reason() == null || request.reason().isBlank() ? "" : ": " + request.reason()),
                    origin.id(), "TRA:" + centralFolio);
            destinationFolio = entry.getFolio();
        } else {
            PosStockMovement entry = posStockService.receiveFromWarehouse(destinationStore.id(), request.articleId(),
                    request.quantity(), centralFolio);
            destinationFolio = entry.getFolio();
        }
        StoreTransfer transfer = transferRepository.saveAndFlush(StoreTransfer.register(
                centralFolio, origin.id(), posCode, request, destinationFolio, clock.instant()));
        auditLogService.record("TRASLADO_TIENDA_REGISTRADO",
                "Traslado " + centralFolio + " (" + request.localFolio() + "): " + request.quantity()
                        + " unidad(es) de " + posCode + " hacia "
                        + (destinationStore == null ? "el almacén central" : request.destination())
                        + ", folio de entrada " + destinationFolio + ".",
                centralFolio, origin.id());
        return transfer;
    }

    /** De dónde viene una recepción: el almacén central (folio SAL) o la tienda que trasladó (folio TRA). */
    private String originOf(String originFolio) {
        if (originFolio == null || !originFolio.startsWith("TRA-")) {
            return StoreTransfer.CENTRAL;
        }
        return transferRepository.findByCentralFolio(originFolio)
                .map(StoreTransfer::getOriginPosCode)
                .orElse(StoreTransfer.CENTRAL);
    }

    @Transactional(readOnly = true)
    public List<StoreInvoice> listInvoices() {
        return invoiceRepository.findAllByOrderByIssuedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<StoreTransfer> listTransfers() {
        return transferRepository.findAllByOrderByOccurredAtDesc();
    }
}
