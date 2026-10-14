package com.megastore.sgologic.credit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "credit_decisions")
class CreditDecision {

    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(name = "requested_amount", nullable = false, updatable = false, precision = 14, scale = 2)
    private BigDecimal requestedAmount;

    @Column(name = "currency", nullable = false, updatable = false, length = 3)
    private String currency;

    @Column(name = "eligible", nullable = false, updatable = false)
    private boolean eligible;

    @Column(name = "approved_limit", nullable = false, updatable = false, precision = 14, scale = 2)
    private BigDecimal approvedLimit;

    @Column(name = "rule_version", nullable = false, updatable = false, length = 64)
    private String ruleVersion;

    @Column(name = "reason_code", nullable = false, updatable = false, length = 64)
    private String reasonCode;

    @Column(name = "inputs_snapshot", updatable = false, columnDefinition = "TEXT")
    private String inputsSnapshot;

    @Column(name = "correlation_id", nullable = false, updatable = false)
    private UUID correlationId;

    @Column(name = "evaluated_at", nullable = false, updatable = false)
    private Instant evaluatedAt;

    protected CreditDecision() {
    }

    static CreditDecision create(UUID customerId, BigDecimal requestedAmount, String currency,
                                 boolean eligible, BigDecimal approvedLimit, String ruleVersion,
                                 String reasonCode, String inputsSnapshot, UUID correlationId,
                                 Instant evaluatedAt) {
        CreditDecision decision = new CreditDecision();
        decision.id = UUID.randomUUID();
        decision.customerId = customerId;
        decision.requestedAmount = requestedAmount;
        decision.currency = currency;
        decision.eligible = eligible;
        decision.approvedLimit = approvedLimit;
        decision.ruleVersion = ruleVersion;
        decision.reasonCode = reasonCode;
        decision.inputsSnapshot = inputsSnapshot;
        decision.correlationId = correlationId;
        decision.evaluatedAt = evaluatedAt;
        return decision;
    }

    UUID getId() { return id; }
    UUID getCustomerId() { return customerId; }
    BigDecimal getRequestedAmount() { return requestedAmount; }
    boolean isEligible() { return eligible; }
    BigDecimal getApprovedLimit() { return approvedLimit; }
    String getRuleVersion() { return ruleVersion; }
    String getReasonCode() { return reasonCode; }
    String getInputsSnapshot() { return inputsSnapshot; }
    Instant getEvaluatedAt() { return evaluatedAt; }
}
