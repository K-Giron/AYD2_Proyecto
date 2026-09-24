package com.megastore.sgologic.audit;

import com.megastore.sgologic.shared.http.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Consulta y exportación de evidencia auditable (AUD-02, CDU-12). Requiere
 * rol FINANCE; cada acceso y cada búsqueda fallida quedan registrados.
 */
@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditQueryService queryService;

    public AuditController(AuditQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/events")
    public List<AuditEventResponse> events(@RequestParam(required = false) UUID orderId,
                                           @RequestParam(required = false) String aggregateType,
                                           @RequestParam(required = false) UUID aggregateId,
                                           HttpServletRequest request) {
        UUID correlationId = correlationId(request);
        if (orderId != null && aggregateType == null && aggregateId == null) {
            return queryService.findByOrder(orderId, correlationId).stream()
                    .map(AuditEventResponse::from).toList();
        }
        if (orderId == null && aggregateType != null && aggregateId != null) {
            return queryService.findByAggregate(aggregateType, aggregateId, correlationId).stream()
                    .map(AuditEventResponse::from).toList();
        }
        throw new InvalidAuditQueryException(
                "Indique orderId o bien aggregateType y aggregateId, sin combinar ambos criterios.");
    }

    @GetMapping("/orders/{orderId}/export")
    public ResponseEntity<String> export(@PathVariable UUID orderId, HttpServletRequest request) {
        String csv = queryService.exportOrderEvidence(orderId, correlationId(request));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-" + orderId + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    private UUID correlationId(HttpServletRequest request) {
        return UUID.fromString(request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME).toString());
    }
}
