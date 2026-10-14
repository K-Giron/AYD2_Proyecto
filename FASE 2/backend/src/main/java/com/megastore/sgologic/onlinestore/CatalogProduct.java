package com.megastore.sgologic.onlinestore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Ficha de un producto de la tienda online; la web solo muestra las publicadas. */
@Entity
@Table(name = "online_catalog_products")
public class CatalogProduct {

    @Id
    @Column(name = "product_id")
    private UUID productId;

    @Column(nullable = false, unique = true, length = 64)
    private String sku;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(length = 80)
    private String category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean published;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CatalogProduct() {
    }

    /** Al recibir por primera vez: datos del central, sin publicar hasta que el administrador lo decida. */
    static CatalogProduct fromReceipt(OnlineReceipt receipt, Instant now) {
        CatalogProduct product = new CatalogProduct();
        product.productId = receipt.getProductId();
        product.sku = receipt.getSku();
        product.name = receipt.getName();
        product.description = receipt.getDescription();
        product.price = receipt.getPrice();
        product.published = false;
        product.createdAt = now;
        product.updatedAt = now;
        return product;
    }

    void edit(String name, String description, String category, BigDecimal price, Instant now) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.updatedAt = now;
    }

    void publish(Instant now) {
        this.published = true;
        this.publishedAt = now;
        this.updatedAt = now;
    }

    void unpublish(Instant now) {
        this.published = false;
        this.updatedAt = now;
    }

    public UUID getProductId() { return productId; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public BigDecimal getPrice() { return price; }
    public boolean isPublished() { return published; }
    public Instant getPublishedAt() { return publishedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
