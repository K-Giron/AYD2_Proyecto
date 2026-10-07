package com.megastore.storeservice.sales;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Ticket de venta de la tienda. La venta es local; la factura se sube después al central. */
@Entity
@Table(name = "sales")
public class Sale {

    public static final String CONSUMIDOR_FINAL = "CF";

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 24)
    private String folio;

    @Column(nullable = false, length = 80)
    private String seller;

    @Column(name = "tax_id", nullable = false, length = 20)
    private String taxId;

    @Column(name = "customer_name", nullable = false, length = 160)
    private String customerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 24)
    private PaymentMethod paymentMethod;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "cash_received", precision = 12, scale = 2)
    private BigDecimal cashReceived;

    @Column(name = "change_given", precision = 12, scale = 2)
    private BigDecimal changeGiven;

    @Column(name = "web_customer", length = 160)
    private String webCustomer;

    @Column(name = "web_cart_id")
    private UUID webCartId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false, length = 16)
    private SaleSyncStatus syncStatus;

    @Column(name = "central_folio", length = 24)
    private String centralFolio;

    @Column(name = "sync_attempts", nullable = false)
    private int syncAttempts;

    @Column(name = "last_sync_error", length = 500)
    private String lastSyncError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "synced_at")
    private Instant syncedAt;

    @Column(name = "reprint_count", nullable = false)
    private int reprintCount;

    @Column(name = "last_reprinted_at")
    private Instant lastReprintedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "sale_lines", joinColumns = @JoinColumn(name = "sale_id"))
    @OrderColumn(name = "line_no")
    private List<SaleLine> lines = new ArrayList<>();

    protected Sale() {
    }

    static Sale register(String folio, String seller, String taxId, String customerName, PaymentMethod paymentMethod,
                         List<SaleLine> lines, BigDecimal cashReceived, String webCustomer, UUID webCartId,
                         Instant now) {
        Sale sale = new Sale();
        sale.id = UUID.randomUUID();
        sale.folio = folio;
        sale.seller = seller;
        sale.taxId = taxId;
        sale.customerName = customerName;
        sale.paymentMethod = paymentMethod;
        sale.lines = new ArrayList<>(lines);
        sale.total = lines.stream().map(SaleLine::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (paymentMethod == PaymentMethod.EFECTIVO) {
            sale.cashReceived = cashReceived;
            sale.changeGiven = cashReceived.subtract(sale.total);
        }
        sale.webCustomer = webCustomer;
        sale.webCartId = webCartId;
        sale.syncStatus = SaleSyncStatus.PENDIENTE;
        sale.createdAt = now;
        return sale;
    }

    void markSent(String centralFolio, Instant now) {
        this.syncStatus = SaleSyncStatus.ENVIADA;
        this.centralFolio = centralFolio;
        this.syncAttempts++;
        this.lastSyncError = null;
        this.syncedAt = now;
    }

    void markAttemptFailed(String error, boolean rejected) {
        this.syncAttempts++;
        this.lastSyncError = error == null ? null : error.substring(0, Math.min(error.length(), 500));
        if (rejected) {
            this.syncStatus = SaleSyncStatus.ERROR;
        }
    }

    void reprinted(Instant now) {
        this.reprintCount++;
        this.lastReprintedAt = now;
    }

    void retry() {
        if (syncStatus == SaleSyncStatus.ERROR) {
            syncStatus = SaleSyncStatus.PENDIENTE;
        }
    }

    public UUID getId() { return id; }
    public String getFolio() { return folio; }
    public String getSeller() { return seller; }
    public String getTaxId() { return taxId; }
    public String getCustomerName() { return customerName; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public BigDecimal getTotal() { return total; }
    public BigDecimal getCashReceived() { return cashReceived; }
    public BigDecimal getChangeGiven() { return changeGiven; }
    public String getWebCustomer() { return webCustomer; }
    public UUID getWebCartId() { return webCartId; }
    public SaleSyncStatus getSyncStatus() { return syncStatus; }
    public String getCentralFolio() { return centralFolio; }
    public int getSyncAttempts() { return syncAttempts; }
    public String getLastSyncError() { return lastSyncError; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSyncedAt() { return syncedAt; }
    public int getReprintCount() { return reprintCount; }
    public Instant getLastReprintedAt() { return lastReprintedAt; }
    public List<SaleLine> getLines() { return List.copyOf(lines); }
}
