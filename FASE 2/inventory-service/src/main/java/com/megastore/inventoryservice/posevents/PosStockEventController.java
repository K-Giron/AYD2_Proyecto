package com.megastore.inventoryservice.posevents;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Seguimiento de los eventos de stock enviados a cada punto de venta (pantalla "Stock por tienda"). */
@RestController
@RequestMapping("/api/v1/pos-stock-events")
class PosStockEventController {

    private final PosStockEventService service;

    PosStockEventController(PosStockEventService service) {
        this.service = service;
    }

    record PosStockEventResponse(UUID id, String posCode, String sku, String articleName, String movement, int delta,
                                 int quantityAfter, String folio, String reference, String status, int attempts,
                                 String lastError, String acknowledgement, Instant createdAt, Instant deliveredAt) {
        static PosStockEventResponse of(PosStockEvent e) {
            return new PosStockEventResponse(e.getId(), e.getPosCode(), e.getSku(), e.getArticleName(), e.getMovement(),
                    e.getDelta(), e.getQuantityAfter(), e.getFolio(), e.getReference(), e.getStatus(), e.getAttempts(),
                    e.getLastError(), e.getAcknowledgement(), e.getCreatedAt(), e.getDeliveredAt());
        }
    }

    @GetMapping
    List<PosStockEventResponse> list(@RequestParam(required = false) String posCode) {
        return service.recent(posCode).stream().map(PosStockEventResponse::of).toList();
    }

    @PostMapping("/{eventId}/retry")
    PosStockEventResponse retry(@PathVariable UUID eventId) {
        return PosStockEventResponse.of(service.retry(eventId));
    }
}
