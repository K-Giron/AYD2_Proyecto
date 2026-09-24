package com.megastore.sgologic.query;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.util.UUID;

/**
 * QRY-01 / CDU-09: consulta consolidada del estado de una orden desde
 * cualquier canal.
 */
@RestController
@RequestMapping("/api/v1")
public class OrderSummaryController {

    private final OrderSummaryQueryService queryService;
    private final Clock clock;

    public OrderSummaryController(OrderSummaryQueryService queryService, Clock clock) {
        this.queryService = queryService;
        this.clock = clock;
    }

    @GetMapping("/orders/{orderId}/summary")
    public OrderSummaryResponse getSummary(@PathVariable UUID orderId) {
        return OrderSummaryResponse.from(queryService.findByOrderId(orderId), clock.instant());
    }

    /**
     * Punto de ingesta manual conservado como apoyo para fuentes que aún no
     * publican eventos. Los estados de Orders ya llegan automáticamente por
     * RabbitMQ con Inbox idempotente; pagos, despacho, entrega e incidencias
     * pueden usar este contrato mientras completan sus productores.
     */
    @PostMapping("/query/order-summary")
    public ResponseEntity<OrderSummaryResponse> upsert(@Valid @RequestBody OrderSummaryUpsertRequest request) {
        OrderSummaryProjection projection = queryService.upsert(
                request.orderId(), request.source(), request.status(), request.asOf());
        return ResponseEntity.ok(OrderSummaryResponse.from(projection, clock.instant()));
    }
}
