package com.megastore.sgologic.incidents;

import com.megastore.sgologic.audit.api.AuditEntryRequest;
import com.megastore.sgologic.audit.api.AuditRecorder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
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

    private final IncidentRepository repository;
    private final AuditRecorder auditRecorder;
    private final Clock clock;

    public IncidentService(IncidentRepository repository, AuditRecorder auditRecorder, Clock clock) {
        this.repository = repository;
        this.auditRecorder = auditRecorder;
        this.clock = clock;
    }

    @Transactional
    public Incident register(UUID orderId, IncidentType type, String reason, boolean partial,
                              List<IncidentLineItem> items, UUID correlationId) {
        Incident incident = repository.save(
                Incident.register(orderId, type, reason, partial, items, correlationId, clock.instant()));
        auditRecorder.record(new AuditEntryRequest(AGGREGATE_TYPE, incident.getId(), orderId,
                "INCIDENT_REGISTERED", incident.getStatus().name(), null,
                state("status", incident.getStatus().name()), correlationId, null, null));
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

    private String state(String field, String value) {
        return "{\"" + field + "\":\"" + value + "\"}";
    }
}
