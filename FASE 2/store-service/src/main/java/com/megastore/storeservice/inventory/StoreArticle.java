package com.megastore.storeservice.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Artículo que la tienda conoce porque el central se lo envió; mismo id que en el central. */
@Entity
@Table(name = "store_articles")
public class StoreArticle {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String sku;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StoreArticle() {
    }

    static StoreArticle fromCentral(UUID id, String sku, String name, String description, BigDecimal price,
                                    Instant now) {
        StoreArticle article = new StoreArticle();
        article.id = id;
        article.sku = sku;
        article.name = name;
        article.description = description;
        article.price = price;
        article.updatedAt = now;
        return article;
    }

    void refresh(String sku, String name, String description, BigDecimal price, Instant now) {
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.price = price;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public Instant getUpdatedAt() { return updatedAt; }
}
