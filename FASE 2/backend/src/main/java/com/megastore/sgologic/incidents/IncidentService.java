package com.megastore.sgologic.incidents;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * INC-01 / CDU-10: registra una devolución o incidencia vinculada a la
 * venta original y conserva la relación completa/parcial. La validación
 * automática de elegibilidad (historial de pago, despacho y entrega) queda
 * pendiente de AUD-01 y QRY-01; mientras tanto, la resolución la decide
 * manualmente Personal de atención (CDU-10, E1).
 */
@Service
public class IncidentService {

    private final IncidentRepository repository;
    private final Clock clock;

    public IncidentService(IncidentRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public Incident register(UUID orderId, IncidentType type, String reason, boolean partial,
                              List<IncidentLineItem> items, UUID correlationId) {
        Incident incident = Incident.register(orderId, type, reason, partial, items, correlationId, clock.instant());
        return repository.save(incident);
    }

    public Incident resolve(UUID id, IncidentStatus resolution, BigDecimal refundAmount) {
        Incident incident = repository.findById(id).orElseThrow(() -> new IncidentNotFoundException(id));
        if (incident.getStatus() == IncidentStatus.APPROVED
                || incident.getStatus() == IncidentStatus.REJECTED
                || incident.getStatus() == IncidentStatus.REFUNDED) {
            throw new IncidentAlreadyResolvedException(id, incident.getStatus());
        }
        incident.resolve(resolution, refundAmount, clock.instant());
        return repository.save(incident);
    }

    public Incident getIncident(UUID id) {
        return repository.findById(id).orElseThrow(() -> new IncidentNotFoundException(id));
    }

    public List<Incident> getIncidentsForOrder(UUID orderId) {
        return repository.findByOrderIdOrderByCreatedAtAsc(orderId);
    }
}
