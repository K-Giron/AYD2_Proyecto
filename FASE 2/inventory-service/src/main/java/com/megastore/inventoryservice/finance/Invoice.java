package com.megastore.inventoryservice.finance;

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

/**
 * Cuenta por pagar (proveedor) o por cobrar (tienda/cliente) del almacén
 * central. Puede referenciar opcionalmente el folio de un movimiento de
 * inventario, sin depender de él.
 */
@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_type", nullable = false, updatable = false, length = 16)
    private InvoiceType type;

    @Column(name = "counterparty", nullable = false, length = 160)
    private String counterparty;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "related_folio", length = 16)
    private String relatedFolio;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private InvoiceStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    protected Invoice() {
        // Requerido por JPA.
    }

    static Invoice create(InvoiceType type, String counterparty, String description, BigDecimal amount,
                           String relatedFolio, LocalDate dueDate, Instant now) {
        Invoice invoice = new Invoice();
        invoice.id = UUID.randomUUID();
        invoice.type = type;
        invoice.counterparty = counterparty;
        invoice.description = description;
        invoice.amount = amount;
        invoice.relatedFolio = relatedFolio;
        invoice.dueDate = dueDate;
        invoice.status = InvoiceStatus.PENDING;
        invoice.createdAt = now;
        return invoice;
    }

    void markPaid(Instant now) {
        if (status == InvoiceStatus.PAID) {
            throw new InvoiceAlreadyPaidException(id);
        }
        status = InvoiceStatus.PAID;
        paidAt = now;
    }

    public UUID getId() { return id; }
    public InvoiceType getType() { return type; }
    public String getCounterparty() { return counterparty; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public String getRelatedFolio() { return relatedFolio; }
    public LocalDate getDueDate() { return dueDate; }
    public InvoiceStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPaidAt() { return paidAt; }
}
