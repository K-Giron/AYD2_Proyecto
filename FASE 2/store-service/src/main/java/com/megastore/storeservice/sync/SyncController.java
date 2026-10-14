package com.megastore.storeservice.sync;

import com.megastore.storeservice.inventory.InventoryService;
import com.megastore.storeservice.sales.SaleService;
import com.megastore.storeservice.shared.StoreBusinessException;
import com.megastore.storeservice.shared.StoreProperties;
import com.megastore.storeservice.transfers.TransferService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/store")
public class SyncController {

    private final SyncService syncService;
    private final SyncLog syncLog;
    private final CentralClient central;
    private final SaleService saleService;
    private final TransferService transferService;
    private final StoreProperties properties;
    private final InventoryService inventoryService;

    public SyncController(SyncService syncService, SyncLog syncLog, CentralClient central, SaleService saleService,
                          TransferService transferService, StoreProperties properties,
                          InventoryService inventoryService) {
        this.inventoryService = inventoryService;
        this.syncService = syncService;
        this.syncLog = syncLog;
        this.central = central;
        this.saleService = saleService;
        this.transferService = transferService;
        this.properties = properties;
    }

    @GetMapping("/info")
    public StoreInfo info() {
        return new StoreInfo(properties.code(), properties.name(), properties.invoice(), syncService.isCentralReachable(),
                syncService.getLastContactAt(), saleService.countPending(), transferService.countPending(),
                inventoryService.countShipmentsInTransit());
    }

    /** Botón "Sincronizar": baja recepciones y sube lo pendiente ahora mismo. */
    @PostMapping("/sync")
    public SyncResult syncNow() {
        try {
            int received = syncService.pullReceipts(true);
            syncService.pushPending();
            return new SyncResult(received, saleService.countPending(), transferService.countPending());
        } catch (CentralUnavailableException | CentralRejectedException error) {
            throw new StoreBusinessException(HttpStatus.BAD_GATEWAY, "CENTRAL_UNAVAILABLE", error.getMessage());
        }
    }

    @GetMapping("/sync-log")
    public List<SyncLogEntry> log() {
        return syncLog.recent();
    }

    /** Destinos de traslado: el central y las demás tiendas que el central conoce. */
    @GetMapping("/destinations")
    public List<CentralClient.PointOfSale> destinations() {
        try {
            return central.pointsOfSale().stream()
                    .filter(pointOfSale -> !pointOfSale.code().equals(properties.code()))
                    .toList();
        } catch (CentralUnavailableException | CentralRejectedException error) {
            throw new StoreBusinessException(HttpStatus.BAD_GATEWAY, "CENTRAL_UNAVAILABLE", error.getMessage());
        }
    }

    public record StoreInfo(String code, String name, StoreProperties.Invoice invoice, boolean centralReachable, Instant lastContactAt,
                            long pendingInvoices, long pendingTransfers, long shipmentsInTransit) {
    }

    public record SyncResult(int receiptsApplied, long pendingInvoices, long pendingTransfers) {
    }
}
