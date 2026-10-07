package com.megastore.sgologic.onlinestore;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;

/**
 * POS de la tienda online: recibe del almacén central (cuenta de servicio,
 * rol POS_EVENTS) cada cambio de existencia de POS-ONLINE con la existencia
 * que el central registra, y lo deja en la bitácora de la tienda online
 * comparado con su almacén CD-WEB. Idempotente por {@code eventId}.
 */
@RestController
@RequestMapping("/api/v1/online-store/stock-events")
class OnlineStockEventController {

    private final JdbcTemplate jdbc;
    private final OnlineCatalogService catalog;
    private final OnlineSyncLog syncLog;
    private final Clock clock;

    OnlineStockEventController(JdbcTemplate jdbc, OnlineCatalogService catalog, OnlineSyncLog syncLog, Clock clock) {
        this.jdbc = jdbc;
        this.catalog = catalog;
        this.syncLog = syncLog;
        this.clock = clock;
    }

    record StockEvent(@NotNull UUID eventId, @NotBlank String posCode, @NotNull UUID articleId, @NotBlank String sku,
                      String name, @NotBlank String movement, int delta, int quantityAfter, @NotBlank String folio,
                      String reference) {
    }

    @PostMapping
    @Transactional
    public Map<String, Object> receive(@Valid @RequestBody StockEvent event) {
        int inserted = jdbc.update("INSERT INTO online_store_stock_events (event_id, folio, received_at) VALUES (?, ?, ?) "
                + "ON CONFLICT DO NOTHING", event.eventId(), event.folio(), Timestamp.from(clock.instant()));
        if (inserted == 1) {
            String change = event.movement() + " " + (event.delta() > 0 ? "+" : "−") + Math.abs(event.delta())
                    + " de " + event.sku() + (event.reference() == null ? "" : " (" + event.reference() + ")");
            if ("ENTRADA".equals(event.movement())) {
                syncLog.record(OnlineSyncLog.BAJADA, "EVENTO_STOCK", event.folio(), true, "Central: " + change
                        + ". Existencia en el central: " + event.quantityAfter() + ". Confirma la recepción del folio para sumarla a CD-WEB.");
            } else {
                OnlineInventoryRow row = catalog.inventory().stream()
                        .filter(r -> r.productId().equals(event.articleId())).findFirst().orElse(null);
                int onHand = row == null ? 0 : row.onHand();
                int inTransit = row == null ? 0 : row.inTransit();
                boolean matches = onHand + inTransit == event.quantityAfter();
                syncLog.record(OnlineSyncLog.BAJADA, "EVENTO_STOCK", event.folio(), matches, "Central: " + change
                        + ". Existencia en el central: " + event.quantityAfter() + "; en CD-WEB: " + onHand
                        + (inTransit > 0 ? " + " + inTransit + " por recibir" : "")
                        + (matches ? ". Cuadra." : ". No cuadra: revisar con el central."));
            }
        }
        return Map.of("received", event.eventId(), "store", "POS-ONLINE");
    }
}
