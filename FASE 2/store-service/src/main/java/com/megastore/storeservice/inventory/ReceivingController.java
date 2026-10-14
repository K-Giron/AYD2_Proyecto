package com.megastore.storeservice.inventory;

import com.megastore.storeservice.sync.SyncLog;
import com.megastore.storeservice.sync.SyncService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * Recepción por folio: la tienda escribe el folio que emitió el central
 * (SAL-…, o TRA-… si viene de otra tienda), ve qué trae y lo confirma.
 * Solo entonces la mercadería suma a su existencia.
 */
@RestController
@RequestMapping("/api/store/receiving")
public class ReceivingController {

    private final InventoryService inventoryService;
    private final SyncService syncService;
    private final SyncLog syncLog;

    public ReceivingController(InventoryService inventoryService, SyncService syncService, SyncLog syncLog) {
        this.inventoryService = inventoryService;
        this.syncService = syncService;
        this.syncLog = syncLog;
    }

    /** Lo que está en camino y aún nadie ha confirmado. */
    @GetMapping
    public List<ShipmentResponse> inTransit() {
        return inventoryService.listReceipts().stream()
                .filter(receipt -> receipt.getStatus() == StoreReceipt.Status.EN_TRANSITO)
                .map(StoreReceipt::getShipmentFolio)
                .distinct()
                .map(folio -> ShipmentResponse.of(folio, inventoryService.findShipment(folio)))
                .toList();
    }

    @GetMapping("/{folio}")
    public ShipmentResponse lookup(@PathVariable String folio) {
        List<StoreReceipt> lines = inventoryService.findShipment(folio);
        if (lines.isEmpty()) {
            // Puede que el central lo acabe de emitir: se le pregunta antes de decir que no existe.
            try {
                syncService.pullReceipts(false);
            } catch (RuntimeException offline) {
                // sin conexión: se responde con lo que la tienda ya conoce
            }
            lines = inventoryService.findShipment(folio);
        }
        return ShipmentResponse.of(folio.trim().toUpperCase(java.util.Locale.ROOT), lines);
    }

    @PostMapping("/{folio}/confirm")
    public ShipmentResponse confirm(@PathVariable String folio, @Valid @RequestBody ConfirmRequest request) {
        List<StoreReceipt> received = inventoryService.confirmShipment(folio, request.seller().trim());
        int units = received.stream().mapToInt(StoreReceipt::getQuantity).sum();
        syncLog.record(SyncLog.BAJADA, "RECEPCION", received.getFirst().getShipmentFolio(), true,
                "Recibido por " + request.seller().trim() + ": " + units + " unidad(es) en " + received.size()
                        + " línea(s) (" + String.join(", ", received.stream()
                        .map(line -> line.getQuantity() + " × " + line.getSku()).toList()) + "). Ya suma a la existencia.");
        return ShipmentResponse.of(received.getFirst().getShipmentFolio(),
                inventoryService.findShipment(received.getFirst().getShipmentFolio()));
    }

    public record ConfirmRequest(@NotBlank @Size(max = 80) String seller) {
    }

    /** Un folio de envío con sus líneas. {@code status}: NO_ENCONTRADO, EN_TRANSITO o RECIBIDO. */
    public record ShipmentResponse(String folio, String status, String origin, Instant sentAt, String receivedBy,
                                   Instant receivedAt, int units, List<StoreReceipt> lines) {

        static ShipmentResponse of(String folio, List<StoreReceipt> lines) {
            if (lines.isEmpty()) {
                return new ShipmentResponse(folio, "NO_ENCONTRADO", null, null, null, null, 0, lines);
            }
            boolean pending = lines.stream().anyMatch(line -> line.getStatus() == StoreReceipt.Status.EN_TRANSITO);
            StoreReceipt first = lines.getFirst();
            return new ShipmentResponse(first.getShipmentFolio(), pending ? "EN_TRANSITO" : "RECIBIDO",
                    first.getOrigin(), first.getOccurredAt(), pending ? null : first.getReceivedBy(),
                    pending ? null : first.getAppliedAt(), lines.stream().mapToInt(StoreReceipt::getQuantity).sum(),
                    lines);
        }
    }
}
