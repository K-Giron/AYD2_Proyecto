package com.megastore.storeservice.sales;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.util.UUID;

@Embeddable
public class SaleLine {

    @Column(name = "article_id", nullable = false)
    private UUID articleId;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    protected SaleLine() {
    }

    static SaleLine of(UUID articleId, String sku, String name, BigDecimal unitPrice, int quantity) {
        SaleLine line = new SaleLine();
        line.articleId = articleId;
        line.sku = sku;
        line.name = name;
        line.unitPrice = unitPrice;
        line.quantity = quantity;
        line.lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        return line;
    }

    public UUID getArticleId() { return articleId; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
    public BigDecimal getLineTotal() { return lineTotal; }
}
