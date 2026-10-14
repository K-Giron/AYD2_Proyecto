package com.megastore.sgologic.integration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "erp_sales")
class ErpSale {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "order_id", nullable = false, updatable = false, unique = true)
    private UUID orderId;

    @Column(name = "net_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal netTotal;

    @Column(name = "tax_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal taxTotal;

    @Column(name = "grand_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal grandTotal;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "confirmed_at", nullable = false, updatable = false)
    private Instant confirmedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ErpSaleStatus status;

    @Column(name = "batch_id")
    private UUID batchId;

    /** WEB (orden del monolito) o STORE (factura de tienda física). */
    @Column(name = "channel", nullable = false, length = 16)
    private String channel;

    @Column(name = "point_of_sale", length = 32)
    private String pointOfSale;

    /** Referencia legible para el ERP: id de la orden o folio de la factura de tienda (FT-…/TKT-…). */
    @Column(name = "source_reference", length = 64)
    private String sourceReference;

    /** PAYKLIK, OWN_CREDIT o STORE; nunca se infiere del estado de cobro. */
    @Column(name = "financial_route", nullable = false, length = 24)
    private String financialRoute;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ErpSale() {
    }

    static ErpSale register(UUID orderId, BigDecimal netTotal, BigDecimal taxTotal,
                            BigDecimal grandTotal, String currency, Instant confirmedAt, Instant now) {
        ErpSale sale = new ErpSale();
        sale.id = UUID.randomUUID();
        sale.orderId = orderId;
        sale.netTotal = netTotal;
        sale.taxTotal = taxTotal;
        sale.grandTotal = grandTotal;
        sale.currency = currency;
        sale.confirmedAt = confirmedAt;
        sale.status = ErpSaleStatus.PENDING;
        sale.channel = "WEB";
        sale.pointOfSale = "POS-ONLINE";
        sale.sourceReference = orderId.toString();
        sale.financialRoute = "PAYKLIK";
        sale.createdAt = now;
        sale.updatedAt = now;
        return sale;
    }

    static ErpSale register(UUID orderId, BigDecimal netTotal, BigDecimal taxTotal, BigDecimal grandTotal,
                            String currency, Instant confirmedAt, String channel, String pointOfSale,
                            String sourceReference, Instant now) {
        return register(orderId, netTotal, taxTotal, grandTotal, currency, confirmedAt,
                channel, pointOfSale, sourceReference,
                "STORE".equals(channel) ? "STORE" : "PAYKLIK", now);
    }

    static ErpSale register(UUID orderId, BigDecimal netTotal, BigDecimal taxTotal, BigDecimal grandTotal,
                            String currency, Instant confirmedAt, String channel, String pointOfSale,
                            String sourceReference, String financialRoute, Instant now) {
        ErpSale sale = register(orderId, netTotal, taxTotal, grandTotal, currency, confirmedAt, now);
        sale.channel = channel;
        sale.pointOfSale = pointOfSale;
        sale.sourceReference = sourceReference;
        sale.financialRoute = financialRoute;
        return sale;
    }

    void includeIn(UUID batchId, Instant now) {
        this.batchId = batchId;
        this.status = ErpSaleStatus.BATCHED;
        this.updatedAt = now;
    }

    void markDelivered(Instant now) {
        this.status = ErpSaleStatus.DELIVERED;
        this.updatedAt = now;
    }

    UUID getId() { return id; }
    UUID getOrderId() { return orderId; }
    BigDecimal getNetTotal() { return netTotal; }
    BigDecimal getTaxTotal() { return taxTotal; }
    BigDecimal getGrandTotal() { return grandTotal; }
    String getCurrency() { return currency; }
    Instant getConfirmedAt() { return confirmedAt; }
    ErpSaleStatus getStatus() { return status; }
    UUID getBatchId() { return batchId; }
    Instant getCreatedAt() { return createdAt; }
    Instant getUpdatedAt() { return updatedAt; }
    String getChannel() { return channel; }
    String getPointOfSale() { return pointOfSale; }
    String getSourceReference() { return sourceReference; }
    String getFinancialRoute() { return financialRoute; }
}
