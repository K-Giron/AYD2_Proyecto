package com.megastore.storeservice.stockevents;

import com.megastore.storeservice.inventory.InventoryService;
import com.megastore.storeservice.shared.StoreProperties;
import com.megastore.storeservice.sync.SyncLog;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;

/**
 * POS local: recibe del almacén central cada cambio de existencia de esta
 * tienda (entrada por folio, venta facturada, traslado) con la existencia que
 * el central quedó registrando, y lo deja en la bitácora comparado con la
 * existencia de la tienda. Idempotente por {@code eventId}.
 */
@RestController
@RequestMapping("/api/store/stock-events")
class StockEventController {

    private final JdbcTemplate jdbc;
    private final InventoryService inventory;
    private final SyncLog syncLog;
    private final StoreProperties properties;
    private final Clock clock;

    StockEventController(JdbcTemplate jdbc, InventoryService inventory, SyncLog syncLog, StoreProperties properties, Clock clock) {
        this.jdbc = jdbc;
        this.inventory = inventory;
        this.syncLog = syncLog;
        this.properties = properties;
        this.clock = clock;
    }

    record StockEvent(@NotNull UUID eventId, @NotBlank String posCode, @NotNull UUID articleId, @NotBlank String sku,
                      String name, @NotBlank String movement, int delta, int quantityAfter, @NotBlank String folio,
                      String reference) {
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> receive(@Valid @RequestBody StockEvent event) {
        int inserted = jdbc.update("INSERT INTO stock_events (event_id, folio, received_at) VALUES (?, ?, ?) ON CONFLICT DO NOTHING",
                event.eventId(), event.folio(), java.sql.Timestamp.from(clock.instant()));
        if (inserted == 1) {
            String change = event.movement() + " " + (event.delta() > 0 ? "+" : "−") + Math.abs(event.delta())
                    + " de " + event.sku() + (event.reference() == null ? "" : " (" + event.reference() + ")");
            if ("ENTRADA".equals(event.movement())) {
                syncLog.record(SyncLog.BAJADA, "EVENTO_STOCK", event.folio(), true, "Central: " + change
                        + ". Existencia en el central: " + event.quantityAfter() + ". Confirma la recepción del folio para sumarla aquí.");
            } else {
                int local = inventory.quantityOf(event.articleId());
                int inTransit = inventory.unitsInTransit(event.articleId());
                boolean matches = local + inTransit == event.quantityAfter();
                syncLog.record(SyncLog.BAJADA, "EVENTO_STOCK", event.folio(), matches, "Central: " + change
                        + ". Existencia en el central: " + event.quantityAfter() + "; en la tienda: " + local
                        + (inTransit > 0 ? " + " + inTransit + " por recibir" : "")
                        + (matches ? ". Cuadra." : ". No cuadra: revisar con el central."));
            }
        }
        return ResponseEntity.ok(Map.of("received", event.eventId(), "store", properties.code()));
    }
}
