package com.megastore.inventoryservice.storesync;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.util.UUID;

@Embeddable
public class StoreInvoiceLine {

    @Column(name = "article_id", nullable = false)
    private UUID articleId;

    @Column(name = "sku", nullable = false, length = 64)
    private String sku;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    protected StoreInvoiceLine() {
        // Requerido por JPA.
    }

    static StoreInvoiceLine of(UploadStoreInvoiceRequest.Line line) {
        StoreInvoiceLine stored = new StoreInvoiceLine();
        stored.articleId = line.articleId();
        stored.sku = line.sku();
        stored.name = line.name();
        stored.unitPrice = line.unitPrice();
        stored.quantity = line.quantity();
        stored.lineTotal = line.lineTotal();
        return stored;
    }

    public UUID getArticleId() { return articleId; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
    public BigDecimal getLineTotal() { return lineTotal; }
}
