package com.megastore.sgologic.payments;

import com.megastore.sgologic.audit.api.AuditEntryRequest;
import com.megastore.sgologic.audit.api.AuditRecorder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;

/**
 * Persiste cada paso del intento en su propia transacción y deja la
 * evidencia auditable en la misma transacción (CDU-03, postcondición).
 * Relee la entidad dentro de cada transacción para no arrastrar versiones
 * desconectadas.
 */
@Service
class PaymentAttemptWriter {

    private static final String AGGREGATE_TYPE = "PaymentAttempt";

    private final PaymentAttemptRepository repository;
    private final AuditRecorder auditRecorder;
    private final Clock clock;

    PaymentAttemptWriter(PaymentAttemptRepository repository, AuditRecorder auditRecorder, Clock clock) {
        this.repository = repository;
        this.auditRecorder = auditRecorder;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    PaymentAttempt create(UUID orderId, PaymentChannel channel, PaymentMethod method, BigDecimal amount,
                          String currency, UUID correlationId, String idempotencyKey) {
        PaymentAttempt attempt = PaymentAttempt.create(UUID.randomUUID(), orderId, channel, method, amount,
                currency, correlationId, idempotencyKey, clock.instant());
        repository.save(attempt);
        auditRecorder.record(new AuditEntryRequest(AGGREGATE_TYPE, attempt.getId(), orderId,
                "PAYMENT_ATTEMPT_CREATED", attempt.getStatus().name(), null,
                state(attempt.getStatus().name(), null), correlationId, null, null));
        return attempt;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    PaymentAttempt markApproved(UUID attemptId, String providerReference, UUID providerAttemptId) {
        PaymentAttempt attempt = load(attemptId);
        String previous = attempt.getStatus().name();
        attempt.markApproved(providerReference, providerAttemptId, clock.instant());
        PaymentAttempt saved = repository.save(attempt);
        auditRecorder.record(new AuditEntryRequest(AGGREGATE_TYPE, attemptId, saved.getOrderId(),
                "PAYMENT_APPROVED", saved.getStatus().name(), state(previous, null),
                state(saved.getStatus().name(), null), saved.getCorrelationId(), null, null));
        return saved;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    PaymentAttempt markRejected(UUID attemptId, String rejectionCode, String message, UUID providerAttemptId) {
        PaymentAttempt attempt = load(attemptId);
        String previous = attempt.getStatus().name();
        attempt.markRejected(rejectionCode, message, providerAttemptId, clock.instant());
        PaymentAttempt saved = repository.save(attempt);
        auditRecorder.record(new AuditEntryRequest(AGGREGATE_TYPE, attemptId, saved.getOrderId(),
                "PAYMENT_REJECTED", saved.getStatus().name(), state(previous, null),
                state(saved.getStatus().name(), rejectionCode), saved.getCorrelationId(), null, null));
        return saved;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    PaymentAttempt markPendingConfirmation(UUID attemptId, String code, String message) {
        PaymentAttempt attempt = load(attemptId);
        attempt.markPendingConfirmation(code, message, clock.instant());
        PaymentAttempt saved = repository.save(attempt);
        auditRecorder.record(new AuditEntryRequest(AGGREGATE_TYPE, attemptId, saved.getOrderId(),
                "PAYMENT_PENDING_CONFIRMATION", saved.getStatus().name(), state("PENDING", null),
                state(saved.getStatus().name(), code), saved.getCorrelationId(), null, null));
        return saved;
    }

    private PaymentAttempt load(UUID attemptId) {
        return repository.findById(attemptId)
                .orElseThrow(() -> new PaymentAttemptNotFoundException(attemptId));
    }

    private String state(String status, String failureCode) {
        if (failureCode == null) {
            return "{\"status\":\"" + status + "\"}";
        }
        return "{\"status\":\"" + status + "\",\"failureCode\":\"" + failureCode + "\"}";
    }
}
