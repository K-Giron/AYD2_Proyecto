package com.megastore.sgologic.audit;

import com.megastore.sgologic.audit.api.AuditEntryRequest;
import com.megastore.sgologic.audit.api.AuditRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * Implementa el registro auditable transversal (AUD-01, CDU-12). La
 * evidencia se escribe en la misma transacción del cambio de negocio y el
 * actor se toma del contexto autenticado; sin autenticación se registra
 * {@code system}.
 */
@Service
public class AuditService implements AuditRecorder {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditEventRepository repository;
    private final Clock clock;

    public AuditService(AuditEventRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public UUID record(AuditEntryRequest request) {
        AuditEvent event = AuditEvent.create(UUID.randomUUID(), request.eventId(), request.aggregateType(),
                request.aggregateId(), request.orderId(), currentActor(), request.action(), request.result(),
                request.beforeState(), request.afterState(), request.correlationId(), request.causationId(),
                clock.instant());
        repository.save(event);
        log.info("Evidencia registrada: action={}, result={}, aggregateType={}, aggregateId={}, orderId={}, "
                        + "actor={}, correlationId={}",
                event.getAction(), event.getResult(), event.getAggregateType(), event.getAggregateId(),
                event.getOrderId(), event.getActor(), event.getCorrelationId());
        return event.getId();
    }

    private String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return "system";
        }
        return authentication.getName();
    }
}
