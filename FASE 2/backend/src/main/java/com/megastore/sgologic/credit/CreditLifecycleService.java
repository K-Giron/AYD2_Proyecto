package com.megastore.sgologic.credit;

import com.megastore.sgologic.credit.api.CreditOperationResult;
import com.megastore.sgologic.credit.api.CreditOperationStatus;
import jakarta.persistence.EntityManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Vida de una obligación después de concederse: vencimiento, pago y anulación.
 * Todo cambio que toca el saldo del cliente se serializa con el mismo bloqueo
 * que usan las concesiones y la reconstrucción del perfil.
 */
@Service
class CreditLifecycleService {

    private final CreditOperationRepository operations;
    private final CreditCustomerProfileRepository profiles;
    private final CreditConcurrencyGuard concurrency;
    private final CreditOperationEvents events;
    private final EntityManager entityManager;
    private final Clock clock;

    CreditLifecycleService(CreditOperationRepository operations, CreditCustomerProfileRepository profiles,
                           CreditConcurrencyGuard concurrency, CreditOperationEvents events, EntityManager entityManager,
                           Clock clock) {
        this.operations = operations;
        this.profiles = profiles;
        this.concurrency = concurrency;
        this.events = events;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    /** El cliente pagó. Repetir el pago de una operación ya pagada no tiene efecto. */
    @Transactional
    CreditOperationResult settle(UUID operationId, UUID correlationId) {
        CreditOperation operation = operations.findById(operationId)
                .orElseThrow(() -> new CreditOperationNotFoundException(operationId));
        concurrency.lockCustomer(operation.getCustomerId());
        // Bajo el bloqueo se relee de la base: otro pago concurrente pudo haberla saldado.
        entityManager.refresh(operation);
        if (operation.getStatus() != CreditOperationStatus.SETTLED) {
            operation.settle(clock.instant());
            release(operation, correlationId);
        }
        return toResult(operation);
    }

    /**
     * La orden se canceló: la obligación se anula y el saldo vuelve al cliente.
     * Es idempotente y no hace nada si no hubo concesión vigente (rechazada,
     * ya pagada, vencida o sin crédito).
     */
    @Transactional
    Optional<CreditOperationResult> cancelForOrder(UUID orderId, UUID correlationId) {
        Optional<CreditOperation> found = operations.findByOrderId(orderId);
        if (found.isEmpty()) return Optional.empty();
        concurrency.lockCustomer(found.get().getCustomerId());
        CreditOperation operation = found.get();
        entityManager.refresh(operation);
        if (operation.getStatus() == CreditOperationStatus.GRANTED) {
            operation.cancel(clock.instant());
            release(operation, correlationId);
        }
        return Optional.of(toResult(operation));
    }

    /** Marca como vencidas las obligaciones vigentes cuyo plazo terminó antes de {@code today}. */
    @Transactional
    int markOverdue(LocalDate today, UUID correlationId) {
        List<CreditOperation> due = operations.findByStatusAndDueDateBefore(CreditOperationStatus.GRANTED, today);
        int changed = 0;
        for (CreditOperation candidate : due) {
            concurrency.lockCustomer(candidate.getCustomerId());
            CreditOperation operation = candidate;
            entityManager.refresh(operation);
            if (operation.getStatus() != CreditOperationStatus.GRANTED) continue;
            Instant now = clock.instant();
            operation.markOverdue(now);
            profiles.findById(operation.getCustomerId()).ifPresent(profile -> {
                profile.updateOverdueFlag(true);
                profiles.save(profile);
            });
            events.publish(operation, availableCreditOf(operation.getCustomerId()), correlationId);
            changed++;
        }
        return changed;
    }

    @Scheduled(fixedDelayString = "${sgo.credit.overdue-check-interval-ms:3600000}",
            initialDelayString = "${sgo.credit.overdue-check-initial-delay-ms:300000}")
    void markOverdueScheduled() {
        markOverdue(LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC), UUID.randomUUID());
    }

    /** Devuelve el monto al saldo, recalcula la marca de mora del cliente y publica el cambio. */
    private void release(CreditOperation operation, UUID correlationId) {
        profiles.findById(operation.getCustomerId()).ifPresent(profile -> {
            profile.restoreAvailableCredit(operation.getAmount());
            profile.updateOverdueFlag(operations.existsByCustomerIdAndStatus(
                    operation.getCustomerId(), CreditOperationStatus.OVERDUE));
            profiles.save(profile);
        });
        operations.flush();
        events.publish(operation, availableCreditOf(operation.getCustomerId()), correlationId);
    }

    private java.math.BigDecimal availableCreditOf(UUID customerId) {
        return profiles.findById(customerId).map(CreditCustomerProfile::getAvailableCredit)
                .orElse(java.math.BigDecimal.ZERO);
    }

    private CreditOperationResult toResult(CreditOperation operation) {
        return new CreditOperationResult(operation.getId(), operation.getDecisionId(),
                operation.getStatus(), operation.getApprovedLimit(), operation.getDueDate(),
                operation.getRuleVersion(), operation.getReasonCode(), operation.getCreatedAt());
    }
}
