package com.megastore.storeservice.posevents;

import com.megastore.storeservice.shared.StoreBusinessException;
import com.megastore.storeservice.shared.StoreProperties;
import com.megastore.storeservice.sync.SyncLog;
import com.megastore.storeservice.webcart.MonolithClient;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PathVariable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * POS local de la tienda: recibe los eventos de actualización que emite
 * SGO-Logic (estado de las órdenes web) y los muestra a la tienda. Idempotente
 * por {@code eventId}: el reintento del emisor no duplica nada.
 */
@RestController
@RequestMapping("/api/store/pos-events")
class PosEventController {

    private final PosEventRepository repository;
    private final StoreProperties properties;
    private final ObjectMapper objectMapper;
    private final MonolithClient monolith;
    private final SyncLog syncLog;
    private final Clock clock;

    PosEventController(PosEventRepository repository, StoreProperties properties, ObjectMapper objectMapper,
                       MonolithClient monolith, SyncLog syncLog, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.monolith = monolith;
        this.syncLog = syncLog;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> receive(@Valid @RequestBody IncomingEvent incoming) {
        if (!repository.existsById(incoming.eventId())) {
            JsonNode payload;
            try {
                payload = objectMapper.readTree(incoming.payload());
            } catch (RuntimeException unreadable) {
                throw new StoreBusinessException(HttpStatus.BAD_REQUEST, "INVALID_POS_EVENT", "El evento no trae un detalle JSON válido.");
            }
            String status = payload.path("status").asString("");
            String pickupStore = payload.path("pickupStore").isNull() ? null : payload.path("pickupStore").asString(null);
            repository.saveAndFlush(PosEvent.received(incoming.eventId(), incoming.eventType(), incoming.orderId(),
                    status.isBlank() ? incoming.eventType() : status, pickupStore, incoming.payload(), clock.instant()));
        }
        return ResponseEntity.ok(Map.of("received", incoming.eventId(), "store", properties.code()));
    }

    /** Pedidos web con su último estado; {@code pickupHere} = el cliente lo recoge en esta tienda. */
    @GetMapping
    @Transactional(readOnly = true)
    public List<WebOrderView> orders() {
        Map<UUID, WebOrderView> latest = new LinkedHashMap<>();
        for (PosEvent event : repository.findTop500ByOrderByReceivedAtDesc()) {
            if (latest.containsKey(event.getOrderId())) {
                continue;
            }
            JsonNode payload = objectMapper.readTree(event.getPayload());
            List<String> items = new ArrayList<>();
            payload.path("items").forEach(item -> items.add(item.path("quantity").asInt() + " × " + item.path("name").asString("")));
            latest.put(event.getOrderId(), new WebOrderView(event.getOrderId(), event.getStatus(),
                    payload.path("total").isNumber() ? payload.path("total").decimalValue().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() : "", items, payload.path("destination").asString(null),
                    properties.code().equals(event.getPickupStore()),
                    payload.path("trackingNumber").isNull() ? null : payload.path("trackingNumber").asString(null),
                    event.getReceivedAt()));
        }
        return List.copyOf(latest.values());
    }

    record IncomingEvent(@NotNull UUID eventId, @NotBlank String eventType, @NotNull UUID orderId, int storeNumber,
                         @NotBlank String payload) {
    }

    record WebOrderView(UUID orderId, String status, String total, List<String> items, String destination,
                        boolean pickupHere, String trackingNumber, Instant lastUpdate) {
    }

    record HandoffRequest(@NotBlank @Size(max = 80) String seller, @NotBlank @Size(max = 120) String receivedBy) {
    }

    /**
     * El cliente llega por su pedido web: la tienda confirma la entrega a
     * SGO-Logic (que valida que el pedido ya llegó y que es de esta tienda).
     */
    @PostMapping("/{orderId}/handoff")
    public Map<String, Object> handOver(@PathVariable UUID orderId, @Valid @RequestBody HandoffRequest request) {
        String reference = orderId.toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT);
        try {
            monolith.handOverPickup(orderId, properties.code(), request.receivedBy().trim());
        } catch (StoreBusinessException rejected) {
            syncLog.record(SyncLog.SUBIDA, "ENTREGA_PEDIDO", reference, false, "Entrega del pedido web rechazada: " + rejected.getMessage());
            throw rejected;
        }
        syncLog.record(SyncLog.SUBIDA, "ENTREGA_PEDIDO", reference, true, "Pedido web entregado al cliente ("
                + request.receivedBy().trim() + ") por " + request.seller().trim() + ".");
        return Map.of("orderId", orderId, "delivered", true);
    }
}
