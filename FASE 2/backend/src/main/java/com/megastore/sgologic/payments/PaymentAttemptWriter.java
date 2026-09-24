package com.megastore.sgologic.payments;

import com.megastore.sgologic.payments.api.PaymentMethod;

import com.megastore.sgologic.audit.api.AuditEntryRequest;
import com.megastore.sgologic.audit.api.AuditRecorder;
import com.megastore.sgologic.integration.outbox.OutboxEventRequest;
import com.megastore.sgologic.integration.outbox.PublishEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;

/**
 * Persiste cada paso del intento en su propia transacción, deja la evidencia
 * auditable y publica el evento correspondiente por Outbox en la misma
 * transacción (ADR-002). Al quedar indeterminado programa la conciliación de
 * PAY-03 dentro de los 60 s. Relee la entidad dentro de cada transacción
 * para no arrastrar versiones desconectadas.
 */
@Service
class PaymentAttemptWriter {

    private static final String AGGREGATE_TYPE = "PaymentAttempt";

    private final PaymentAttemptRepository repository;
    private final PaymentReconciliationRepository reconciliationRepository;
    private final PaymentReconciliationProperties reconciliationProperties;
    private final AuditRecorder auditRecorder;
    private final PublishEvent publishEvent;
    private final Clock clock;

    PaymentAttemptWriter(PaymentAttemptRepository repository,
                         PaymentReconciliationRepository reconciliationRepository,
                         PaymentReconciliationProperties reconciliationProperties,
                         AuditRecorder auditRecorder, PublishEvent publishEvent, Clock clock) {
        this.repository = repository;
        this.reconciliationRepository = reconciliationRepository;
        this.reconciliationProperties = reconciliationProperties;
        this.auditRecorder = auditRecorder;
        this.publishEvent = publishEvent;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    PaymentAttempt create(UUID orderId, PaymentChannel channel, PaymentMethod method, BigDecimal amount,
                          String currency, UUID correlationId, String idempotencyKey, String paymentReference) {
        PaymentAttempt attempt = PaymentAttempt.create(UUID.randomUUID(), orderId, channel, method, amount,
                currency, correlationId, idempotencyKey, paymentReference, clock.instant());
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
        publishEvent.publish(new OutboxEventRequest(AGGREGATE_TYPE, attemptId, "PaymentApproved", 1,
                saved.getCorrelationId(), null, payload(saved, null)));
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
        publishEvent.publish(new OutboxEventRequest(AGGREGATE_TYPE, attemptId, "PaymentRejected", 1,
                saved.getCorrelationId(), null, payload(saved, rejectionCode)));
        return saved;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    PaymentAttempt markIndeterminate(UUID attemptId, String code, String message) {
        PaymentAttempt attempt = load(attemptId);
        attempt.markIndeterminate(code, message, clock.instant());
        PaymentAttempt saved = repository.save(attempt);
        auditRecorder.record(new AuditEntryRequest(AGGREGATE_TYPE, attemptId, saved.getOrderId(),
                "PAYMENT_INDETERMINATE", saved.getStatus().name(), state("PENDING", null),
                state(saved.getStatus().name(), code), saved.getCorrelationId(), null, null));
        // El evento sale en la misma transacción del cambio de estado
        // (ADR-002): la Saga y PAY-03 no dependen de leer la tabla.
        publishEvent.publish(new OutboxEventRequest(AGGREGATE_TYPE, attemptId, "PaymentIndeterminate", 1,
                saved.getCorrelationId(), null, payload(saved, code)));
        scheduleReconciliation(saved);
        return saved;
    }

    /**
     * Programa la conciliación (PAY-03) con un primer trabajo dentro de los
     * 60 s. El índice único parcial impide dos trabajos abiertos por intento.
     */
    private void scheduleReconciliation(PaymentAttempt attempt) {
        if (reconciliationRepository
                .findByPaymentAttemptIdAndStatus(attempt.getId(), PaymentReconciliationStatus.PENDING)
                .isPresent()) {
            return;
        }
        reconciliationRepository.save(PaymentReconciliation.schedule(attempt.getId(),
                clock.instant().plus(reconciliationProperties.getFirstDelay()), clock.instant()));
    }

    private PaymentEventPayload payload(PaymentAttempt attempt, String failureCode) {
        return new PaymentEventPayload(attempt.getId(), attempt.getOrderId(), attempt.getAmount(),
                attempt.getCurrency(), attempt.getChannel().name(), attempt.getMethod().name(),
                attempt.getStatus().name(), attempt.getProviderReference(), failureCode);
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
