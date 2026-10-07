package com.megastore.inventoryservice.storesync;

import com.megastore.inventoryservice.article.Article;
import com.megastore.inventoryservice.article.ArticleService;
import com.megastore.inventoryservice.security.StoreAccountGuard;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/store-sync")
public class StoreSyncController {

    private final StoreSyncService service;
    private final ArticleService articleService;
    private final StoreAccountGuard storeAccountGuard;

    public StoreSyncController(StoreSyncService service, ArticleService articleService,
                               StoreAccountGuard storeAccountGuard) {
        this.service = service;
        this.articleService = articleService;
        this.storeAccountGuard = storeAccountGuard;
    }

    @GetMapping("/{posCode}/receipts")
    public List<StoreReceiptResponse> receipts(@PathVariable String posCode,
                                               @RequestParam(required = false)
                                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since,
                                               Authentication authentication) {
        storeAccountGuard.assertCanActFor(authentication.getName(), posCode);
        return service.receipts(posCode, since);
    }

    @PostMapping("/{posCode}/invoices")
    public ResponseEntity<Map<String, Object>> uploadInvoice(@PathVariable String posCode,
                                                             @Valid @RequestBody UploadStoreInvoiceRequest request,
                                                             Authentication authentication) {
        storeAccountGuard.assertCanActFor(authentication.getName(), posCode);
        StoreInvoice invoice = service.uploadInvoice(posCode, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "centralFolio", invoice.getCentralFolio(),
                "ticketFolio", invoice.getTicketFolio(),
                "receivedAt", invoice.getReceivedAt()));
    }

    @PostMapping("/{posCode}/transfers")
    public ResponseEntity<StoreTransferResponse> registerTransfer(@PathVariable String posCode,
                                                                  @Valid @RequestBody RegisterStoreTransferRequest request,
                                                                  Authentication authentication) {
        storeAccountGuard.assertCanActFor(authentication.getName(), posCode);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(service.registerTransfer(posCode, request)));
    }

    /** Facturas de tienda recibidas; con {@code since}, solo las recibidas desde ese instante (para el ERP). */
    @GetMapping("/invoices")
    public List<StoreInvoiceResponse> invoices(@RequestParam(required = false)
                                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since) {
        return service.listInvoices().stream()
                .filter(invoice -> since == null || !invoice.getReceivedAt().isBefore(since))
                .map(StoreInvoiceResponse::from).toList();
    }

    @GetMapping("/transfers")
    public List<StoreTransferResponse> transfers() {
        return service.listTransfers().stream().map(this::toResponse).toList();
    }

    private StoreTransferResponse toResponse(StoreTransfer transfer) {
        Article article = articleService.getById(transfer.getArticleId());
        return new StoreTransferResponse(transfer.getCentralFolio(), transfer.getLocalFolio(),
                transfer.getOriginPosCode(), transfer.getDestination(), transfer.getDestinationFolio(),
                transfer.getArticleId(), article.getSku(), article.getName(), transfer.getQuantity(),
                transfer.getReason(), transfer.getSeller(), transfer.getOccurredAt());
    }

    public record StoreTransferResponse(String centralFolio, String localFolio, String originPosCode,
                                        String destination, String destinationFolio, UUID articleId, String sku,
                                        String articleName, int quantity, String reason, String seller,
                                        Instant occurredAt) {
    }

    public record StoreInvoiceResponse(UUID id, String centralFolio, String ticketFolio, String pointOfSaleCode,
                                       Instant issuedAt, Instant receivedAt, String seller, String taxId,
                                       String customerName, String paymentMethod, BigDecimal total,
                                       List<StoreInvoiceLine> lines) {
        static StoreInvoiceResponse from(StoreInvoice invoice) {
            return new StoreInvoiceResponse(invoice.getId(), invoice.getCentralFolio(), invoice.getTicketFolio(),
                    invoice.getPointOfSaleCode(), invoice.getIssuedAt(), invoice.getReceivedAt(), invoice.getSeller(),
                    invoice.getTaxId(), invoice.getCustomerName(), invoice.getPaymentMethod(), invoice.getTotal(),
                    invoice.getLines());
        }
    }
}
