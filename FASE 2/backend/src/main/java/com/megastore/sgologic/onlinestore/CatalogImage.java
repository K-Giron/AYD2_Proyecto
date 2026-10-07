package com.megastore.sgologic.onlinestore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "online_catalog_images")
public class CatalogImage {

    @Id
    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "content_type", nullable = false, length = 40)
    private String contentType;

    @Column(nullable = false)
    private byte[] data;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CatalogImage() {
    }

    static CatalogImage of(UUID productId, String contentType, byte[] data, Instant now) {
        CatalogImage image = new CatalogImage();
        image.productId = productId;
        image.contentType = contentType;
        image.data = data;
        image.updatedAt = now;
        return image;
    }

    void replace(String contentType, byte[] data, Instant now) {
        this.contentType = contentType;
        this.data = data;
        this.updatedAt = now;
    }

    public UUID getProductId() { return productId; }
    public String getContentType() { return contentType; }
    public byte[] getData() { return data; }
    public Instant getUpdatedAt() { return updatedAt; }
}
