package com.megastore.sgologic.payments;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Trabajo de conciliación de un intento indeterminado (PAY-03, CDU-03 E1).
 * Reutiliza la misma {@code idempotencyKey} del intento: conciliar nunca
 * genera un segundo cobro.
 */
@Entity
@Table(name = "payment_reconciliations")
public class PaymentReconciliation {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "payment_attempt_id", nullable = false, updatable = false)
    private UUID paymentAttemptId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private PaymentReconciliationStatus status;

    @Column(name = "result", length = 64)
    private String result;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "failure_code", length = 64)
    private String failureCode;

    @Column(name = "failure_message", length = 500)
    private String failureMessage;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected PaymentReconciliation() {
        // Requerido por JPA.
    }

    static PaymentReconciliation schedule(UUID paymentAttemptId, Instant scheduledAt, Instant now) {
        PaymentReconciliation reconciliation = new PaymentReconciliation();
        reconciliation.id = UUID.randomUUID();
        reconciliation.paymentAttemptId = paymentAttemptId;
        reconciliation.status = PaymentReconciliationStatus.PENDING;
        reconciliation.attempts = 0;
        reconciliation.scheduledAt = scheduledAt;
        reconciliation.createdAt = now;
        reconciliation.updatedAt = now;
        return reconciliation;
    }

    /**
     * Registra un intento fallido. Devuelve {@code true} si queda
     * reprogramado con el siguiente backoff; {@code false} cuando se agota
     * y pasa a la bandeja de fallidos.
     */
    boolean registerFailure(String code, String message, Instant now, List<Duration> retryDelays, int maxAttempts) {
        attempts += 1;
        failureCode = code;
        failureMessage = message == null ? null : message.substring(0, Math.min(message.length(), 500));
        updatedAt = now;
        if (attempts < maxAttempts) {
            Duration delay = retryDelays.get(Math.min(attempts - 1, retryDelays.size() - 1));
            status = PaymentReconciliationStatus.PENDING;
            scheduledAt = now.plus(delay);
            return true;
        }
        status = PaymentReconciliationStatus.DEAD_LETTER;
        return false;
    }

    void resolve(String outcome, Instant now) {
        status = PaymentReconciliationStatus.RESOLVED;
        result = outcome;
        failureCode = null;
        failureMessage = null;
        updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getPaymentAttemptId() { return paymentAttemptId; }
    public PaymentReconciliationStatus getStatus() { return status; }
    public String getResult() { return result; }
    public int getAttempts() { return attempts; }
    public String getFailureCode() { return failureCode; }
    public String getFailureMessage() { return failureMessage; }
    public Instant getScheduledAt() { return scheduledAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
