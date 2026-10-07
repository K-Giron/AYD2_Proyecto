package com.megastore.sgologic.query;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * QRY-01 / CDU-09: mantiene y expone el modelo de lectura consolidado. La
 * búsqueda es por clave primaria indexada, compatible con el límite de
 * EaC-01 (≤ 5 s); no agrega trabajo adicional en el camino de lectura.
 */
@Service
public class OrderSummaryQueryService {

    private final OrderSummaryProjectionRepository repository;
    private final Clock clock;

    public OrderSummaryQueryService(OrderSummaryProjectionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * Actualiza una fuente de la proyección. Orders invoca este contrato desde
     * su consumidor RabbitMQ; el endpoint manual sigue disponible para las
     * fuentes cuyos productores de eventos aún están pendientes.
     */
    @org.springframework.transaction.annotation.Transactional
    public OrderSummaryProjection upsert(UUID orderId, ProjectionSource source, String status, Instant asOf) {
        Instant now = clock.instant();
        repository.insertIfAbsent(orderId, now);
        OrderSummaryProjection projection = repository.findForUpdate(orderId)
                .orElseThrow(() -> new IllegalStateException("La proyección de " + orderId + " no se pudo crear."));
        projection.apply(source, status, asOf == null ? now : asOf, now);
        return repository.save(projection);
    }

    public OrderSummaryProjection findByOrderId(UUID orderId) {
        return repository.findById(orderId).orElseThrow(() -> new OrderSummaryNotFoundException(orderId));
    }
}
