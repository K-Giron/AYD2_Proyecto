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

/**
 * Intento de pago propio del módulo (CDU-03). Orders y PayKlik se
 * referencian solo por identificadores; nunca se almacena PAN completo ni
 * CVV (EaC-12).
 */
@Entity
@Table(name = "payment_attempts")
public class PaymentAttempt {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, updatable = false, length = 32)
    private PaymentChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, updatable = false, length = 32)
    private PaymentMethod method;

    @Column(name = "amount", nullable = false, updatable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, updatable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private PaymentAttemptStatus status;

    @Column(name = "provider_reference", length = 100)
    private String providerReference;

    @Column(name = "provider_attempt_id")
    private UUID providerAttemptId;

    @Column(name = "failure_code", length = 64)
    private String failureCode;

    @Column(name = "failure_message", length = 500)
    private String failureMessage;

    @Column(name = "idempotency_key", nullable = false, updatable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "correlation_id", nullable = false, updatable = false)
    private UUID correlationId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected PaymentAttempt() {
        // Requerido por JPA.
    }

    static PaymentAttempt create(UUID id, UUID orderId, PaymentChannel channel, PaymentMethod method,
                                 BigDecimal amount, String currency, UUID correlationId,
                                 String idempotencyKey, Instant now) {
        PaymentAttempt attempt = new PaymentAttempt();
        attempt.id = id;
        attempt.orderId = orderId;
        attempt.channel = channel;
        attempt.method = method;
        attempt.amount = amount;
        attempt.currency = currency.trim().toUpperCase();
        attempt.status = PaymentAttemptStatus.PENDING;
        attempt.correlationId = correlationId;
        attempt.idempotencyKey = idempotencyKey;
        attempt.createdAt = now;
        attempt.updatedAt = now;
        return attempt;
    }

    void markApproved(String providerReference, UUID providerAttemptId, Instant now) {
        this.status = PaymentAttemptStatus.APPROVED;
        this.providerReference = providerReference;
        this.providerAttemptId = providerAttemptId;
        this.failureCode = null;
        this.failureMessage = null;
        this.updatedAt = now;
    }

    void markRejected(String rejectionCode, String message, UUID providerAttemptId, Instant now) {
        this.status = PaymentAttemptStatus.REJECTED;
        this.providerAttemptId = providerAttemptId;
        this.failureCode = rejectionCode;
        this.failureMessage = message;
        this.updatedAt = now;
    }

    void markIndeterminate(String code, String message, Instant now) {
        this.status = PaymentAttemptStatus.INDETERMINATE;
        this.failureCode = code;
        this.failureMessage = message;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public PaymentChannel getChannel() { return channel; }
    public PaymentMethod getMethod() { return method; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public PaymentAttemptStatus getStatus() { return status; }
    public String getProviderReference() { return providerReference; }
    public UUID getProviderAttemptId() { return providerAttemptId; }
    public String getFailureCode() { return failureCode; }
    public String getFailureMessage() { return failureMessage; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public UUID getCorrelationId() { return correlationId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
