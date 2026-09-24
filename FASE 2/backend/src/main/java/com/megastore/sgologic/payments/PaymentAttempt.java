package com.megastore.sgologic.payments;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_attempts")
public class PaymentAttempt {
    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(nullable = false, updatable = false, length = 24)
    private String channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 24)
    private PaymentMethod method;

    @Column(nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, updatable = false, length = 3)
    private String currency;

    @Column(name = "payment_reference", nullable = false, updatable = false, length = 180)
    private String paymentReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentStatus status;

    @Column(name = "provider_reference", length = 120)
    private String providerReference;

    @Column(name = "result_code", length = 80)
    private String resultCode;

    @Column(name = "result_message", length = 500)
    private String resultMessage;

    @Column(name = "idempotency_key", nullable = false, updatable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "correlation_id", nullable = false, updatable = false)
    private UUID correlationId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "reconciliation_at")
    private Instant reconciliationAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected PaymentAttempt() {
    }

    static PaymentAttempt pending(PaymentProcessingRequest request, Instant now) {
        PaymentAttempt attempt = new PaymentAttempt();
        attempt.id = UUID.randomUUID();
        attempt.orderId = request.orderId();
        attempt.channel = request.channel().trim().toUpperCase();
        attempt.method = request.method();
        attempt.amount = request.amount();
        attempt.currency = request.currency().trim().toUpperCase();
        attempt.paymentReference = request.paymentReference().trim();
        attempt.status = PaymentStatus.PENDING;
        attempt.idempotencyKey = request.idempotencyKey().trim();
        attempt.correlationId = request.correlationId();
        attempt.createdAt = now;
        return attempt;
    }

    void approve(String providerReference, Instant processedAt) {
        this.status = PaymentStatus.APPROVED;
        this.providerReference = providerReference;
        this.resultCode = "APPROVED";
        this.resultMessage = "Pago aprobado por PayKlik";
        this.processedAt = processedAt;
        this.reconciliationAt = null;
    }

    void reject(String code, Instant processedAt) {
        this.status = PaymentStatus.REJECTED;
        this.resultCode = code;
        this.resultMessage = "Pago rechazado por PayKlik";
        this.processedAt = processedAt;
        this.reconciliationAt = null;
    }

    void markIndeterminate(String code, String message, Instant now, Instant reconciliationAt) {
        this.status = PaymentStatus.INDETERMINATE;
        this.resultCode = code;
        this.resultMessage = message;
        this.processedAt = now;
        this.reconciliationAt = reconciliationAt;
    }

    PaymentProcessingResult toResult() {
        return new PaymentProcessingResult(id, orderId, status, providerReference, resultCode, processedAt);
    }

    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public String getChannel() { return channel; }
    public PaymentMethod getMethod() { return method; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getPaymentReference() { return paymentReference; }
    public PaymentStatus getStatus() { return status; }
    public String getProviderReference() { return providerReference; }
    public String getResultCode() { return resultCode; }
    public String getResultMessage() { return resultMessage; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public UUID getCorrelationId() { return correlationId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getProcessedAt() { return processedAt; }
    public Instant getReconciliationAt() { return reconciliationAt; }
}
