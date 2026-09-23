package com.megastore.sgologic.fiscal;

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

/** Agregado fiscal propio de CDU-04; Orders se referencia solo por ID. */
@Entity
@Table(name = "fiscal_documents")
public class FiscalDocument {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "payment_attempt_id", nullable = false, updatable = false)
    private UUID paymentAttemptId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 24)
    private FiscalDocumentType documentType;

    @Column(name = "original_document_id")
    private UUID originalDocumentId;

    @Column(name = "tax_identifier", nullable = false, length = 32)
    private String taxIdentifier;

    @Column(name = "customer_name", nullable = false, length = 200)
    private String customerName;

    @Column(name = "total", nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "cui", nullable = false, unique = true, length = 64)
    private String cui;

    @Column(name = "series", nullable = false, length = 32)
    private String series;

    @Column(name = "xml_payload", nullable = false, columnDefinition = "TEXT")
    private String xmlPayload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private FiscalStatus status;

    @Column(name = "sat_authorization", length = 100)
    private String satAuthorization;

    @Column(name = "failure_code", length = 64)
    private String failureCode;

    @Column(name = "failure_message", length = 500)
    private String failureMessage;

    @Column(name = "correlation_id", nullable = false, updatable = false)
    private UUID correlationId;

    @Column(name = "idempotency_key", nullable = false, updatable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "submission_attempts", nullable = false)
    private int submissionAttempts;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected FiscalDocument() {
        // Requerido por JPA.
    }

    static FiscalDocument create(UUID id, UUID orderId, UUID paymentAttemptId,
                                 FiscalDocumentType documentType, UUID originalDocumentId,
                                 String taxIdentifier, String customerName, BigDecimal total,
                                 String currency, String cui, String series, String xmlPayload,
                                 UUID correlationId, String idempotencyKey, Instant now) {
        FiscalDocument document = new FiscalDocument();
        document.id = id;
        document.orderId = orderId;
        document.paymentAttemptId = paymentAttemptId;
        document.documentType = documentType;
        document.originalDocumentId = originalDocumentId;
        document.taxIdentifier = taxIdentifier;
        document.customerName = customerName;
        document.total = total;
        document.currency = currency;
        document.cui = cui;
        document.series = series;
        document.xmlPayload = xmlPayload;
        document.status = FiscalStatus.FISCAL_PENDING;
        document.correlationId = correlationId;
        document.idempotencyKey = idempotencyKey;
        document.createdAt = now;
        document.updatedAt = now;
        return document;
    }

    void markAttempt(Instant now) {
        submissionAttempts += 1;
        failureCode = null;
        failureMessage = null;
        updatedAt = now;
    }

    void approve(String authorization, Instant now) {
        status = FiscalStatus.APPROVED;
        satAuthorization = authorization;
        failureCode = null;
        failureMessage = null;
        confirmedAt = now;
        updatedAt = now;
    }

    void reject(String code, String message, Instant now) {
        status = FiscalStatus.REJECTED;
        failureCode = code;
        failureMessage = message;
        updatedAt = now;
    }

    void remainPending(String code, String message, Instant now) {
        status = FiscalStatus.FISCAL_PENDING;
        failureCode = code;
        failureMessage = message;
        updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public UUID getPaymentAttemptId() { return paymentAttemptId; }
    public FiscalDocumentType getDocumentType() { return documentType; }
    public UUID getOriginalDocumentId() { return originalDocumentId; }
    public String getTaxIdentifier() { return taxIdentifier; }
    public String getCustomerName() { return customerName; }
    public BigDecimal getTotal() { return total; }
    public String getCurrency() { return currency; }
    public String getCui() { return cui; }
    public String getSeries() { return series; }
    public String getXmlPayload() { return xmlPayload; }
    public FiscalStatus getStatus() { return status; }
    public String getSatAuthorization() { return satAuthorization; }
    public String getFailureCode() { return failureCode; }
    public String getFailureMessage() { return failureMessage; }
    public UUID getCorrelationId() { return correlationId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public int getSubmissionAttempts() { return submissionAttempts; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getConfirmedAt() { return confirmedAt; }
}
