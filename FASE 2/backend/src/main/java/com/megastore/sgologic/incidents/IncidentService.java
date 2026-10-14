package com.megastore.sgologic.incidents;

import com.megastore.sgologic.audit.api.AuditEntryRequest;
import com.megastore.sgologic.audit.api.AuditRecorder;
import com.megastore.sgologic.integration.outbox.OutboxEventRequest;
import com.megastore.sgologic.integration.outbox.PublishEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * INC-01 / CDU-10: registra una devolución o incidencia vinculada a la
 * venta original y conserva la relación completa/parcial. Cada operación
 * deja evidencia auditable transversal (AUD-01) en la misma transacción. La
 * validación automática de elegibilidad (historial de pago, despacho y
 * entrega) queda pendiente de QRY-01; mientras tanto, la resolución la
 * decide manualmente Personal de atención (CDU-10, E1).
 */
@Service
public class IncidentService {

    private static final String AGGREGATE_TYPE = "Incident";
    /** Tipo de evento de dominio publicado por INC-01 (CUN-11: aviso de incidencia registrada). */
    static final String INCIDENT_REGISTERED = "INCIDENT_REGISTERED";

    private final IncidentRepository repository;
    private final AuditRecorder auditRecorder;
    private final PublishEvent eventPublisher;
    private final Clock clock;

    public IncidentService(IncidentRepository repository, AuditRecorder auditRecorder,
                            PublishEvent eventPublisher, Clock clock) {
        this.repository = repository;
        this.auditRecorder = auditRecorder;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public Incident register(UUID orderId, UUID customerId, IncidentType type, String reason, boolean partial,
                              List<IncidentLineItem> items, UUID correlationId) {
        Incident incident = repository.save(
                Incident.register(orderId, customerId, type, reason, partial, items, correlationId, clock.instant()));
        auditRecorder.record(new AuditEntryRequest(AGGREGATE_TYPE, incident.getId(), orderId,
                INCIDENT_REGISTERED, incident.getStatus().name(), null,
                state("status", incident.getStatus().name()), correlationId, null, null));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("incidentId", incident.getId());
        payload.put("orderId", orderId);
        payload.put("type", type.name());
        payload.put("status", incident.getStatus().name());
        eventPublisher.publish(new OutboxEventRequest("INCIDENT", incident.getId(), INCIDENT_REGISTERED,
                1, correlationId, correlationId, payload));
        return incident;
    }

    @Transactional
    public Incident resolve(UUID id, IncidentStatus resolution, BigDecimal refundAmount) {
        Incident incident = repository.findById(id).orElseThrow(() -> new IncidentNotFoundException(id));
        if (incident.getStatus() == IncidentStatus.APPROVED
                || incident.getStatus() == IncidentStatus.REJECTED
                || incident.getStatus() == IncidentStatus.REFUNDED) {
            throw new IncidentAlreadyResolvedException(id, incident.getStatus());
        }
        String previousStatus = incident.getStatus().name();
        incident.resolve(resolution, refundAmount, clock.instant());
        Incident saved = repository.save(incident);
        auditRecorder.record(new AuditEntryRequest(AGGREGATE_TYPE, id, saved.getOrderId(),
                "INCIDENT_RESOLVED", resolution.name(), state("status", previousStatus),
                state("status", resolution.name()), saved.getCorrelationId(), null, null));
        return saved;
    }

    public Incident getIncident(UUID id) {
        return repository.findById(id).orElseThrow(() -> new IncidentNotFoundException(id));
    }

    public List<Incident> getIncidentsForOrder(UUID orderId) {
        return repository.findByOrderIdOrderByCreatedAtAsc(orderId);
    }

    /** Historial de atenciones del cliente a través de todas sus órdenes. */
    public List<Incident> getIncidentsForCustomer(UUID customerId) {
        return repository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    private String state(String field, String value) {
        return "{\"" + field + "\":\"" + value + "\"}";
    }
}
