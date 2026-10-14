package com.megastore.sgologic.credit;

import com.megastore.sgologic.credit.api.CreditOperationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "credit_operations")
class CreditOperation {

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false, updatable = false, unique = true)
    private UUID orderId;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(name = "decision_id", nullable = false, updatable = false)
    private UUID decisionId;

    @Column(name = "amount", nullable = false, updatable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, updatable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private CreditOperationStatus status;

    @Column(name = "overdue_at")
    private Instant overdueAt;

    @Column(name = "settled_at")
    private Instant settledAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "approved_limit", nullable = false, updatable = false, precision = 14, scale = 2)
    private BigDecimal approvedLimit;

    @Column(name = "due_date", updatable = false)
    private LocalDate dueDate;

    @Column(name = "rule_version", nullable = false, updatable = false, length = 64)
    private String ruleVersion;

    @Column(name = "reason_code", nullable = false, updatable = false, length = 64)
    private String reasonCode;

    @Column(name = "idempotency_key", nullable = false, updatable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "correlation_id", nullable = false, updatable = false)
    private UUID correlationId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CreditOperation() {
    }

    static CreditOperation create(UUID orderId, UUID customerId, CreditDecision decision,
                                  BigDecimal amount, String currency, LocalDate dueDate,
                                  String idempotencyKey, UUID correlationId, Instant createdAt) {
        CreditOperation operation = new CreditOperation();
        operation.id = UUID.randomUUID();
        operation.orderId = orderId;
        operation.customerId = customerId;
        operation.decisionId = decision.getId();
        operation.amount = amount;
        operation.currency = currency;
        operation.status = decision.isEligible()
                ? CreditOperationStatus.GRANTED : CreditOperationStatus.REJECTED;
        operation.approvedLimit = decision.getApprovedLimit();
        operation.dueDate = decision.isEligible() ? dueDate : null;
        operation.ruleVersion = decision.getRuleVersion();
        operation.reasonCode = decision.getReasonCode();
        operation.idempotencyKey = idempotencyKey;
        operation.correlationId = correlationId;
        operation.createdAt = createdAt;
        return operation;
    }

    /** Pasa a vencida; solo una operación vigente puede vencer. */
    void markOverdue(Instant at) {
        requireStatus(CreditOperationStatus.GRANTED, CreditOperationStatus.OVERDUE);
        status = CreditOperationStatus.OVERDUE;
        overdueAt = at;
    }

    /** Pagada por el cliente, vigente o ya vencida. */
    void settle(Instant at) {
        requireStatus(CreditOperationStatus.GRANTED, CreditOperationStatus.OVERDUE);
        status = CreditOperationStatus.SETTLED;
        settledAt = at;
    }

    /** Anulada por cancelación de la orden; solo antes de que venza o se pague. */
    void cancel(Instant at) {
        requireStatus(CreditOperationStatus.GRANTED);
        status = CreditOperationStatus.CANCELLED;
        cancelledAt = at;
    }

    private void requireStatus(CreditOperationStatus... allowed) {
        for (CreditOperationStatus candidate : allowed) {
            if (status == candidate) return;
        }
        throw new IllegalStateException("La operación de crédito está " + status
                + " y no admite esta transición.");
    }

    UUID getId() { return id; }
    UUID getOrderId() { return orderId; }
    UUID getCustomerId() { return customerId; }
    BigDecimal getAmount() { return amount; }
    String getCurrency() { return currency; }
    UUID getDecisionId() { return decisionId; }
    CreditOperationStatus getStatus() { return status; }
    BigDecimal getApprovedLimit() { return approvedLimit; }
    LocalDate getDueDate() { return dueDate; }
    String getRuleVersion() { return ruleVersion; }
    String getReasonCode() { return reasonCode; }
    Instant getCreatedAt() { return createdAt; }
    UUID getCorrelationId() { return correlationId; }
}
