package com.megastore.inventoryservice.storesync;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Factura emitida en una tienda física y subida al central después de la venta. */
@Entity
@Table(name = "store_invoices")
public class StoreInvoice {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "central_folio", nullable = false, updatable = false, length = 16)
    private String centralFolio;

    @Column(name = "point_of_sale_id", nullable = false, updatable = false)
    private UUID pointOfSaleId;

    @Column(name = "point_of_sale_code", nullable = false, updatable = false, length = 32)
    private String pointOfSaleCode;

    @Column(name = "ticket_folio", nullable = false, updatable = false, length = 24)
    private String ticketFolio;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @Column(name = "seller", nullable = false, updatable = false, length = 80)
    private String seller;

    @Column(name = "tax_id", nullable = false, updatable = false, length = 20)
    private String taxId;

    @Column(name = "customer_name", nullable = false, updatable = false, length = 160)
    private String customerName;

    @Column(name = "payment_method", nullable = false, updatable = false, length = 24)
    private String paymentMethod;

    @Column(name = "total", nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "store_invoice_lines", joinColumns = @JoinColumn(name = "invoice_id"))
    private List<StoreInvoiceLine> lines = new ArrayList<>();

    protected StoreInvoice() {
        // Requerido por JPA.
    }

    static StoreInvoice receive(String centralFolio, UUID pointOfSaleId, String pointOfSaleCode,
                                UploadStoreInvoiceRequest request, Instant now) {
        StoreInvoice invoice = new StoreInvoice();
        invoice.id = UUID.randomUUID();
        invoice.centralFolio = centralFolio;
        invoice.pointOfSaleId = pointOfSaleId;
        invoice.pointOfSaleCode = pointOfSaleCode;
        invoice.ticketFolio = request.ticketFolio();
        invoice.issuedAt = request.issuedAt();
        invoice.receivedAt = now;
        invoice.seller = request.seller();
        invoice.taxId = request.taxId();
        invoice.customerName = request.customerName();
        invoice.paymentMethod = request.paymentMethod();
        invoice.total = request.total();
        request.lines().forEach(line -> invoice.lines.add(StoreInvoiceLine.of(line)));
        return invoice;
    }

    public UUID getId() { return id; }
    public String getCentralFolio() { return centralFolio; }
    public UUID getPointOfSaleId() { return pointOfSaleId; }
    public String getPointOfSaleCode() { return pointOfSaleCode; }
    public String getTicketFolio() { return ticketFolio; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getReceivedAt() { return receivedAt; }
    public String getSeller() { return seller; }
    public String getTaxId() { return taxId; }
    public String getCustomerName() { return customerName; }
    public String getPaymentMethod() { return paymentMethod; }
    public BigDecimal getTotal() { return total; }
    public List<StoreInvoiceLine> getLines() { return List.copyOf(lines); }
}
