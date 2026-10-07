package com.megastore.inventoryservice.article;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Artículo del catálogo propio del almacén central. */
@Entity
@Table(name = "articles")
public class Article {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "sku", nullable = false, updatable = false, unique = true, length = 64)
    private String sku;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Article() {
        // Requerido por JPA.
    }

    static Article create(String sku, String name, String description, BigDecimal price, Instant now) {
        Article article = new Article();
        article.id = UUID.randomUUID();
        article.sku = sku;
        article.name = name;
        article.description = description;
        article.price = price;
        article.createdAt = now;
        return article;
    }

    public UUID getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
