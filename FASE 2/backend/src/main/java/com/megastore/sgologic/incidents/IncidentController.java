package com.megastore.sgologic.incidents;

import com.megastore.sgologic.shared.http.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** INC-01 / CDU-10: devoluciones, reembolsos e incidencias. */
@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping
    public ResponseEntity<IncidentResponse> register(
            @Valid @RequestBody RegisterIncidentRequest request, HttpServletRequest httpRequest) {
        UUID correlationId = UUID.fromString(
                httpRequest.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME).toString());
        Incident incident = incidentService.register(
                request.orderId(), request.type(), request.reason(), request.partial(),
                request.items().stream().map(IncidentLineItemDto::toDomain).toList(), correlationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(IncidentResponse.from(incident));
    }

    @GetMapping("/{id}")
    public IncidentResponse getIncident(@PathVariable UUID id) {
        return IncidentResponse.from(incidentService.getIncident(id));
    }

    @GetMapping
    public List<IncidentResponse> getIncidentsForOrder(@RequestParam UUID orderId) {
        return incidentService.getIncidentsForOrder(orderId).stream().map(IncidentResponse::from).toList();
    }

    @PatchMapping("/{id}/resolution")
    public IncidentResponse resolve(@PathVariable UUID id, @Valid @RequestBody ResolveIncidentRequest request) {
        return IncidentResponse.from(
                incidentService.resolve(id, request.resolution(), request.refundAmount()));
    }
}
