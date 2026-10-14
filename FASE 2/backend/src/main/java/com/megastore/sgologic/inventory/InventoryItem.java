package com.megastore.sgologic.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * Stock por producto y ubicación (CDU-02, INV-01). {@code locationId}
 * referencia {@link InventoryLocation} dentro de este mismo módulo;
 * {@code productId} es un identificador de demostración: SGO-Logic no
 * gestiona catálogo de productos (DDA §2.2), por lo que Inventory no valida
 * ni es propietario de esa entidad, solo del stock disponible para ella.
 *
 * <p>{@code quantityReserved <= quantityOnHand} se garantiza también a
 * nivel de base de datos (ver migración V1); la disponibilidad se deriva
 * siempre como {@code quantityOnHand - quantityReserved}, nunca se
 * persiste por separado, para no poder desincronizarse.</p>
 */
@Entity
@Table(name = "inventory_items")
public class InventoryItem {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(name = "location_id", nullable = false, updatable = false)
    private UUID locationId;

    @Column(name = "quantity_on_hand", nullable = false)
    private int quantityOnHand;

    @Column(name = "quantity_reserved", nullable = false)
    private int quantityReserved;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected InventoryItem() {
        // Requerido por JPA.
    }

    /**
     * Fila en cero para un producto que llega por primera vez a una
     * ubicación (ver {@link InventoryMovementService#receiveAtLocation}).
     */
    static InventoryItem empty(UUID productId, UUID locationId, Instant now) {
        InventoryItem item = new InventoryItem();
        item.id = UUID.randomUUID();
        item.productId = productId;
        item.locationId = locationId;
        item.quantityOnHand = 0;
        item.quantityReserved = 0;
        item.updatedAt = now;
        return item;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public UUID getLocationId() {
        return locationId;
    }

    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    public int getQuantityReserved() {
        return quantityReserved;
    }

    /** Disponible = en existencia - reservado. Nunca se persiste por separado. */
    public int getQuantityAvailable() {
        return quantityOnHand - quantityReserved;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
