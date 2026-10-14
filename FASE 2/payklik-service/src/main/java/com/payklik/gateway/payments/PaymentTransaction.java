package com.payklik.gateway.payments;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class PaymentTransaction {

    public static final String APPROVED = "APROBADA";
    public static final String REJECTED = "RECHAZADA";

    @Id
    private UUID id;

    @Column(nullable = false)
    private String merchant;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "order_reference", nullable = false)
    private String orderReference;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String method;

    @Column(name = "payment_reference", nullable = false)
    private String paymentReference;

    @Column(nullable = false)
    private String status;

    @Column(name = "authorization_code")
    private String authorizationCode;

    @Column(name = "rejection_code")
    private String rejectionCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PaymentTransaction() {
    }

    static PaymentTransaction approved(String merchant, String idempotencyKey, String orderReference, BigDecimal amount,
                                       String currency, String method, String paymentReference, String authorizationCode,
                                       Instant now) {
        PaymentTransaction tx = base(merchant, idempotencyKey, orderReference, amount, currency, method, paymentReference, now);
        tx.status = APPROVED;
        tx.authorizationCode = authorizationCode;
        return tx;
    }

    static PaymentTransaction rejected(String merchant, String idempotencyKey, String orderReference, BigDecimal amount,
                                       String currency, String method, String paymentReference, String rejectionCode,
                                       Instant now) {
        PaymentTransaction tx = base(merchant, idempotencyKey, orderReference, amount, currency, method, paymentReference, now);
        tx.status = REJECTED;
        tx.rejectionCode = rejectionCode;
        return tx;
    }

    private static PaymentTransaction base(String merchant, String idempotencyKey, String orderReference, BigDecimal amount,
                                           String currency, String method, String paymentReference, Instant now) {
        PaymentTransaction tx = new PaymentTransaction();
        tx.id = UUID.randomUUID();
        tx.merchant = merchant;
        tx.idempotencyKey = idempotencyKey;
        tx.orderReference = orderReference;
        tx.amount = amount;
        tx.currency = currency;
        tx.method = method;
        tx.paymentReference = paymentReference;
        tx.createdAt = now;
        return tx;
    }

    public UUID getId() { return id; }
    public String getMerchant() { return merchant; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getOrderReference() { return orderReference; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getMethod() { return method; }
    public String getPaymentReference() { return paymentReference; }
    public String getStatus() { return status; }
    public String getAuthorizationCode() { return authorizationCode; }
    public String getRejectionCode() { return rejectionCode; }
    public Instant getCreatedAt() { return createdAt; }
}
