package com.megastore.sgologic.onlinestore;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Administración de la tienda online (rol ONLINE_STORE): recepción por folio y catálogo. */
@RestController
@RequestMapping("/api/v1/online-store")
class OnlineStoreAdminController {

    private final OnlineReceivingService receivingService;
    private final OnlineCatalogService catalogService;
    private final OnlineTransferService transferService;
    private final OnlineSyncLog syncLog;
    private final OnlineStoreProperties properties;

    OnlineStoreAdminController(OnlineReceivingService receivingService, OnlineCatalogService catalogService,
                               OnlineTransferService transferService, OnlineSyncLog syncLog,
                               OnlineStoreProperties properties) {
        this.transferService = transferService;
        this.syncLog = syncLog;
        this.receivingService = receivingService;
        this.catalogService = catalogService;
        this.properties = properties;
    }

    @GetMapping("/info")
    public Info info() {
        return new Info(properties.getPointOfSaleCode(), properties.getLocationCode(),
                receivingService.isCentralReachable(), receivingService.getLastContactAt(),
                receivingService.inTransit().size());
    }

    /** "Sincronizar ahora": baja del central lo que se le envió a la tienda online. */
    @PostMapping("/sync")
    public Map<String, Integer> sync() {
        int newShipments = receivingService.pullReceipts();
        transferService.pushPending();
        return Map.of("newShipments", newShipments);
    }

    @GetMapping("/receiving")
    public List<ShipmentResponse> inTransit() {
        return receivingService.inTransit();
    }

    @GetMapping("/receiving/history")
    public List<OnlineReceipt> history() {
        return receivingService.history();
    }

    @GetMapping("/receiving/{folio}")
    public ShipmentResponse lookup(@PathVariable String folio) {
        return findAskingCentral(folio);
    }

    @PostMapping("/receiving/{folio}/confirm")
    public ShipmentResponse confirm(@PathVariable String folio, Authentication authentication) {
        findAskingCentral(folio);
        return receivingService.confirm(folio, authentication == null ? "tienda-online" : authentication.getName());
    }

    /** Si la tienda aún no conoce el folio, puede que el central lo acabe de emitir: se le pregunta antes de decir que no existe. */
    private ShipmentResponse findAskingCentral(String folio) {
        ShipmentResponse shipment = receivingService.find(folio);
        if (!"NO_ENCONTRADO".equals(shipment.status())) {
            return shipment;
        }
        try {
            receivingService.pullReceipts();
        } catch (OnlineStoreException offline) {
            return shipment;
        }
        return receivingService.find(folio);
    }

    /** Kárdex del almacén de la tienda online: entradas, salidas y reservas (activas, vendidas, liberadas). */
    @GetMapping("/movements")
    public List<OnlineMovementRow> movements() {
        return catalogService.movements();
    }

    @GetMapping("/inventory")
    public List<OnlineInventoryRow> inventory() {
        return catalogService.inventory();
    }

    /** Traslado por folio: sale de CD-WEB al emitirlo y se reporta al central enseguida (o queda pendiente). */
    @PostMapping("/transfers")
    public OnlineTransfer createTransfer(@Valid @RequestBody CreateOnlineTransferRequest request,
                                         Authentication authentication) {
        OnlineTransfer transfer = transferService.create(request,
                authentication == null ? "tienda-online" : authentication.getName());
        transferService.push(transfer.getId());
        return transferService.get(transfer.getId());
    }

    @GetMapping("/transfers")
    public List<OnlineTransfer> transfers() {
        return transferService.list();
    }

    @GetMapping("/destinations")
    public List<CentralReceiptsClient.PointOfSale> destinations() {
        return transferService.destinations();
    }

    @GetMapping("/sync-log")
    public List<OnlineSyncLogEntry> syncLog() {
        return syncLog.recent();
    }

    @GetMapping("/products")
    public List<CatalogProductResponse> products() {
        return catalogService.adminList();
    }

    @PutMapping("/products/{productId}")
    public CatalogProductResponse update(@PathVariable UUID productId,
                                         @Valid @RequestBody UpdateCatalogProductRequest request) {
        return catalogService.update(productId, request);
    }

    /** El cuerpo es el archivo de imagen tal cual (JPG, PNG o WebP, máx. 2 MB), con su Content-Type. */
    @PutMapping(value = "/products/{productId}/image", consumes = {"image/jpeg", "image/png", "image/webp"})
    public CatalogProductResponse uploadImage(@PathVariable UUID productId,
                                              @RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType,
                                              @RequestBody byte[] data) {
        return catalogService.setImage(productId, contentType, data);
    }

    @DeleteMapping("/products/{productId}/image")
    public CatalogProductResponse deleteImage(@PathVariable UUID productId) {
        return catalogService.deleteImage(productId);
    }

    @PostMapping("/products/{productId}/publish")
    public CatalogProductResponse publish(@PathVariable UUID productId) {
        return catalogService.publish(productId);
    }

    @PostMapping("/products/{productId}/unpublish")
    public CatalogProductResponse unpublish(@PathVariable UUID productId) {
        return catalogService.unpublish(productId);
    }

    record Info(String pointOfSaleCode, String locationCode, boolean centralReachable, Instant lastContactAt,
                int shipmentsInTransit) {
    }
}
