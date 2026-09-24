package com.megastore.sgologic.payments;

import com.megastore.sgologic.audit.api.AuditEntryRequest;
import com.megastore.sgologic.audit.api.AuditRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * PAY-03 / CDU-03 E1: concilia los intentos indeterminados reutilizando la
 * misma {@code idempotencyKey}, de modo que el mismo intento nunca genera un
 * segundo cobro. El primer trabajo se programa dentro de los 60 s
 * ({@link PaymentAttemptWriter}); los fallos reintentan con backoff y, si el
 * caso sigue sin resolver después de {@code alert-after}, queda en la
 * bandeja de fallidos con alerta para operación.
 */
@Service
public class PaymentReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(PaymentReconciliationService.class);

    private static final String ALERT_AGGREGATE = "PaymentReconciliation";

    private final PaymentReconciliationRepository reconciliationRepository;
    private final PaymentAttemptRepository attemptRepository;
    private final ResilientPayKlikClient payKlikClient;
    private final PaymentAttemptWriter writer;
    private final PaymentReconciliationProperties properties;
    private final AuditRecorder auditRecorder;
    private final Clock clock;

    public PaymentReconciliationService(PaymentReconciliationRepository reconciliationRepository,
                                        PaymentAttemptRepository attemptRepository,
                                        ResilientPayKlikClient payKlikClient, PaymentAttemptWriter writer,
                                        PaymentReconciliationProperties properties, AuditRecorder auditRecorder,
                                        Clock clock) {
        this.reconciliationRepository = reconciliationRepository;
        this.attemptRepository = attemptRepository;
        this.payKlikClient = payKlikClient;
        this.writer = writer;
        this.properties = properties;
        this.auditRecorder = auditRecorder;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${sgo.payments.reconciliation.poll-interval-ms:5000}")
    @Transactional
    public void reconcileDue() {
        if (!properties.isEnabled()) {
            return;
        }
        recoverStalePending();
        List<PaymentReconciliation> due = reconciliationRepository.lockDueBatch(
                clock.instant(), properties.getBatchSize());
        for (PaymentReconciliation reconciliation : due) {
            reconcile(reconciliation);
        }
        if (!due.isEmpty()) {
            log.debug("Conciliación procesó {} trabajo(s) vencido(s).", due.size());
        }
    }

    /**
     * A5: un intento que quedó PENDING más allá del umbral (proceso caído o
     * excepción entre la creación y el resultado) se marca INDETERMINATE y
     * entra al circuito de conciliación, para no quedar huérfano.
     */
    private void recoverStalePending() {
        Instant threshold = clock.instant().minus(properties.getStalePendingAfter());
        List<PaymentAttempt> stale = attemptRepository.findByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
                PaymentAttemptStatus.PENDING, threshold, PageRequest.of(0, properties.getBatchSize()));
        for (PaymentAttempt attempt : stale) {
            writer.markIndeterminate(attempt.getId(), "STALE_PENDING",
                    "Intento pendiente sin resultado; recuperado por el barrido de conciliación.");
            log.warn("Intento {} recuperado como INDETERMINATE por barrido de PENDING viejo (orderId={}).",
                    attempt.getId(), attempt.getOrderId());
        }
    }

    private void reconcile(PaymentReconciliation reconciliation) {
        PaymentAttempt attempt = attemptRepository.findById(reconciliation.getPaymentAttemptId()).orElse(null);
        if (attempt == null) {
            reconciliation.resolve("ATTEMPT_NOT_FOUND", clock.instant());
            reconciliationRepository.save(reconciliation);
            log.warn("Conciliación sin intento asociado: {}", reconciliation.getPaymentAttemptId());
            return;
        }
        if (attempt.getStatus() != PaymentAttemptStatus.INDETERMINATE) {
            // Ya resuelto por otra vía: conciliar es un no-op y no se cobra de nuevo.
            reconciliation.resolve(attempt.getStatus().name(), clock.instant());
            reconciliationRepository.save(reconciliation);
            log.info("Conciliación sin efecto: el intento {} ya está {}.", attempt.getId(), attempt.getStatus());
            return;
        }
        try {
            PayKlikGatewayPort.AuthorizationResult result = payKlikClient.authorize(
                    new PayKlikGatewayPort.AuthorizationRequest(attempt.getOrderId(), attempt.getAmount(),
                            attempt.getCurrency(), reference(attempt), attempt.getCorrelationId(),
                            attempt.getIdempotencyKey()));
            if (result.approved()) {
                writer.markApproved(attempt.getId(), result.providerReference(), result.paymentAttemptId());
                reconciliation.resolve("APPROVED", clock.instant());
            } else {
                writer.markRejected(attempt.getId(), result.rejectionCode(), null, result.paymentAttemptId());
                reconciliation.resolve("REJECTED", clock.instant());
            }
            reconciliationRepository.save(reconciliation);
            log.info("Intento {} conciliado como {}.", attempt.getId(), reconciliation.getResult());
        } catch (PayKlikGatewayPort.PayKlikTimeoutException | PayKlikGatewayPort.PayKlikUnavailableException
                 | PayKlikCircuitOpenException failure) {
            handleFailure(reconciliation, attempt, failureCode(failure), failure.getMessage());
        }
    }

    private void handleFailure(PaymentReconciliation reconciliation, PaymentAttempt attempt,
                               String code, String message) {
        boolean retrying = reconciliation.registerFailure(code, message, clock.instant(),
                properties.getRetryDelays(), properties.getMaxAttempts());
        reconciliationRepository.save(reconciliation);
        long elapsedSeconds = Duration.between(reconciliation.getCreatedAt(), clock.instant()).toSeconds();
        boolean alert = !retrying || elapsedSeconds >= properties.getAlertAfter().toSeconds();
        if (retrying) {
            long delaySeconds = Duration.between(clock.instant(), reconciliation.getScheduledAt()).toSeconds();
            log.warn("Conciliación del intento {} falló ({}); reintento {} programado en {} s.",
                    attempt.getId(), code, reconciliation.getAttempts(), delaySeconds);
            if (elapsedSeconds >= properties.getAlertAfter().toSeconds()) {
                log.error("Alerta a operación: intento {} sigue sin conciliar tras {} s; se mantiene la orden "
                        + "y no se generó un segundo cobro.", attempt.getId(), elapsedSeconds);
            }
        } else {
            log.error("Alerta a operación: intento {} quedó en la bandeja de fallidos tras {} intentos ({}); "
                    + "requiere revisión manual y no se generó un segundo cobro.",
                    attempt.getId(), reconciliation.getAttempts(), code);
        }
        if (alert) {
            auditRecorder.record(new AuditEntryRequest(ALERT_AGGREGATE, reconciliation.getId(),
                    attempt.getOrderId(), "PAYMENT_RECONCILIATION_ALERT",
                    retrying ? "RETRYING_AFTER_ALERT" : "DEAD_LETTER", null,
                    "{\"attempts\":" + reconciliation.getAttempts() + ",\"failureCode\":\"" + code + "\"}",
                    attempt.getCorrelationId(), null, null));
        }
    }

    private String reference(PaymentAttempt attempt) {
        if (attempt.getPaymentReference() == null || attempt.getPaymentReference().isBlank()) {
            return "reconcile-" + attempt.getId();
        }
        return attempt.getPaymentReference();
    }

    private String failureCode(RuntimeException failure) {
        if (failure instanceof PayKlikCircuitOpenException) {
            return "PAYKLIK_CIRCUIT_OPEN";
        }
        if (failure instanceof PayKlikGatewayPort.PayKlikTimeoutException) {
            return "PAYKLIK_TIMEOUT";
        }
        return "PAYKLIK_UNAVAILABLE";
    }
}
