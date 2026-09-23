package com.megastore.sgologic.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * Reserva de una cantidad de un producto en una ubicación para una orden
 * (CDU-02, INV-02). Referencia {@code orderId} únicamente por su UUID:
 * Inventory no lee ni escribe tablas de Orders.
 */
@Entity
@Table(name = "inventory_reservations")
public class InventoryReservation {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(name = "location_id", nullable = false, updatable = false)
    private UUID locationId;

    @Column(name = "quantity", nullable = false, updatable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private InventoryReservationStatus status;

    @Column(name = "correlation_id", nullable = false, updatable = false)
    private UUID correlationId;

    @Column(name = "idempotency_key", nullable = false, updatable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected InventoryReservation() {
        // Requerido por JPA.
    }

    private InventoryReservation(UUID id, UUID orderId, UUID productId, UUID locationId, int quantity,
                                  UUID correlationId, String idempotencyKey, Instant expiresAt, Instant now) {
        this.id = id;
        this.orderId = orderId;
        this.productId = productId;
        this.locationId = locationId;
        this.quantity = quantity;
        this.status = InventoryReservationStatus.ACTIVE;
        this.correlationId = correlationId;
        this.idempotencyKey = idempotencyKey;
        this.expiresAt = expiresAt;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Crea el registro de una reserva ya concedida: la cantidad debe
     * haberse comprometido en {@code inventory_items} de forma atómica
     * (ver {@link InventoryItemRepository#tryReserve}) antes de llamar a
     * este método. Esta clase no repite esa validación.
     */
    static InventoryReservation create(UUID orderId, UUID productId, UUID locationId, int quantity,
                                        UUID correlationId, String idempotencyKey, Instant expiresAt, Instant now) {
        return new InventoryReservation(UUID.randomUUID(), orderId, productId, locationId, quantity,
                correlationId, idempotencyKey, expiresAt, now);
    }

    public void markReleased(Instant now) {
        this.status = InventoryReservationStatus.RELEASED;
        this.updatedAt = now;
    }

    public void markExpired(Instant now) {
        this.status = InventoryReservationStatus.EXPIRED;
        this.updatedAt = now;
    }

    public void markConfirmed(Instant now) {
        this.status = InventoryReservationStatus.CONFIRMED;
        this.updatedAt = now;
    }

    public boolean isActive() {
        return status == InventoryReservationStatus.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getProductId() {
        return productId;
    }

    public UUID getLocationId() {
        return locationId;
    }

    public int getQuantity() {
        return quantity;
    }

    public InventoryReservationStatus getStatus() {
        return status;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
