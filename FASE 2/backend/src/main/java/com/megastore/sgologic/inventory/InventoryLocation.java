package com.megastore.sgologic.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Ubicación autoritativa de inventario (ADR-011): centro de distribución o
 * tienda. Los datos semilla de las 34 ubicaciones se cargan en INV-01
 * (ver {@code db/migration/V2__inventory_seed_locations_and_stock.sql}).
 */
@Entity
@Table(name = "inventory_locations")
public class InventoryLocation {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "code", nullable = false, updatable = false, unique = true, length = 16)
    private String code;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, updatable = false, length = 32)
    private InventoryLocationType type;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected InventoryLocation() {
        // Requerido por JPA.
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public InventoryLocationType getType() {
        return type;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
