package com.megastore.inventoryservice.auditlog;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Bitácora transversal: cualquier módulo puede dejar traza de un evento
 * (folio de entrada/salida, envío, cuenta) sin depender de los detalles
 * internos de quien la originó.
 */
@Service
public class AuditLogService {

    private final AuditLogRepository repository;
    private final Clock clock;

    public AuditLogService(AuditLogRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public void record(String eventType, String description, String relatedFolio, UUID pointOfSaleId) {
        repository.save(AuditLogEntry.record(eventType, description, relatedFolio, pointOfSaleId,
                currentActor(), clock.instant()));
    }

    private String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "system";
    }

    @Transactional(readOnly = true)
    public List<AuditLogEntry> list(String relatedFolio, UUID pointOfSaleId) {
        if (relatedFolio != null) {
            return repository.findByRelatedFolioOrderByOccurredAtDesc(relatedFolio);
        }
        if (pointOfSaleId != null) {
            return repository.findByPointOfSaleIdOrderByOccurredAtDesc(pointOfSaleId);
        }
        return repository.findAllByOrderByOccurredAtDesc();
    }
}
