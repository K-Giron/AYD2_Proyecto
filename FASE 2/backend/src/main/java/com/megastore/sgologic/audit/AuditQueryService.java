package com.megastore.sgologic.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Consulta indexada de evidencia (AUD-02, CDU-12): recupera la historia por
 * orden o por agregado/transacción, registra cada acceso y la búsqueda
 * fallida, y exporta la evidencia en CSV.
 *
 * <p>Los métodos no abren una transacción propia: cada lectura usa la
 * transacción corta del repositorio y la evidencia se escribe con
 * {@link AuditEvidenceWriter} ({@code REQUIRES_NEW}). Así una consulta
 * concurrente nunca retiene una conexión mientras espera otra, y el
 * registro de la búsqueda fallida sobrevive al error (CDU-12 E1). Las
 * consultas por orden devuelven la evidencia de negocio; los eventos de
 * acceso/exportación se consultan por el agregado {@code AuditEvidence}.</p>
 */
@Service
public class AuditQueryService {

    private static final String ACCESS_AGGREGATE = "AuditEvidence";

    private static final Logger log = LoggerFactory.getLogger(AuditQueryService.class);

    private final AuditEventRepository repository;
    private final AuditEvidenceWriter evidenceWriter;

    public AuditQueryService(AuditEventRepository repository, AuditEvidenceWriter evidenceWriter) {
        this.repository = repository;
        this.evidenceWriter = evidenceWriter;
    }

    public List<AuditEvent> findByOrder(UUID orderId, UUID correlationId) {
        List<AuditEvent> events = repository.findByOrderIdAndAggregateTypeNotOrderByOccurredAtAsc(
                orderId, ACCESS_AGGREGATE);
        if (events.isEmpty()) {
            recordSearchFailed("orderId=" + orderId, orderId, orderId, correlationId);
            throw new AuditEvidenceNotFoundException("orderId=" + orderId);
        }
        evidenceWriter.recordQueryEvent("AUDIT_EVIDENCE_ACCESSED", "OK", orderId, orderId, correlationId);
        return events;
    }

    public List<AuditEvent> findByAggregate(String aggregateType, UUID aggregateId, UUID correlationId) {
        List<AuditEvent> events = repository
                .findByAggregateTypeAndAggregateIdOrderByOccurredAtAsc(aggregateType, aggregateId);
        if (events.isEmpty()) {
            recordSearchFailed("aggregateType=" + aggregateType + ", aggregateId=" + aggregateId,
                    aggregateId, null, correlationId);
            throw new AuditEvidenceNotFoundException("aggregateType=" + aggregateType + ", aggregateId=" + aggregateId);
        }
        evidenceWriter.recordQueryEvent("AUDIT_EVIDENCE_ACCESSED", "OK", aggregateId, null, correlationId);
        return events;
    }

    public String exportOrderEvidence(UUID orderId, UUID correlationId) {
        List<AuditEvent> events = repository.findByOrderIdAndAggregateTypeNotOrderByOccurredAtAsc(
                orderId, ACCESS_AGGREGATE);
        if (events.isEmpty()) {
            recordSearchFailed("orderId=" + orderId, orderId, orderId, correlationId);
            throw new AuditEvidenceNotFoundException("orderId=" + orderId);
        }
        evidenceWriter.recordQueryEvent("AUDIT_EVIDENCE_EXPORTED", "OK", orderId, orderId, correlationId);
        return toCsv(events);
    }

    private void recordSearchFailed(String criterion, UUID aggregateId, UUID orderId, UUID correlationId) {
        evidenceWriter.recordQueryEvent("AUDIT_SEARCH_FAILED", "NOT_FOUND", aggregateId, orderId, correlationId);
        log.warn("Alerta a Finanzas: búsqueda de evidencia sin resultados ({}), correlationId={}",
                criterion, correlationId);
    }

    private String toCsv(List<AuditEvent> events) {
        StringBuilder csv = new StringBuilder(
                "eventId,occurredAt,actor,action,result,aggregateType,aggregateId,orderId,correlationId,"
                        + "beforeState,afterState\n");
        for (AuditEvent event : events) {
            csv.append(quote(event.getEventId().toString())).append(',')
                    .append(quote(event.getOccurredAt().toString())).append(',')
                    .append(quote(event.getActor())).append(',')
                    .append(quote(event.getAction())).append(',')
                    .append(quote(event.getResult())).append(',')
                    .append(quote(event.getAggregateType())).append(',')
                    .append(quote(event.getAggregateId().toString())).append(',')
                    .append(quote(event.getOrderId() == null ? "" : event.getOrderId().toString())).append(',')
                    .append(quote(event.getCorrelationId().toString())).append(',')
                    .append(quote(event.getBeforeState())).append(',')
                    .append(quote(event.getAfterState())).append('\n');
        }
        return csv.toString();
    }

    private String quote(String value) {
        if (value == null) {
            return "\"\"";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
