package com.megastore.sgologic.inventorysync;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.util.Map;

/**
 * La terminal de tienda física registra aquí, tras el checkout y antes de
 * correr la Saga, en qué tienda se cobró la orden. Idempotente: repetir el
 * mismo registro devuelve el existente.
 */
@RestController
@RequestMapping("/api/v1/inventory-sync/order-origins")
class OrderOriginController {

    private final OrderOriginRepository repository;
    private final Clock clock;

    OrderOriginController(OrderOriginRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterOrderOriginRequest request) {
        OrderOrigin origin = repository.findById(request.orderId())
                .orElseGet(() -> repository.saveAndFlush(
                        OrderOrigin.of(request.orderId(), request.pointOfSaleCode(), clock.instant())));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "orderId", origin.getOrderId(),
                "pointOfSaleCode", origin.getPointOfSaleCode(),
                "registeredAt", origin.getRegisteredAt()));
    }
}
