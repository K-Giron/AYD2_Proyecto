package com.megastore.sgologic.orders;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Agregado del carrito único y recuperable entre canales de CDU-01. */
@Entity
@Table(name = "carts")
public class Cart {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private CartStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin_channel", nullable = false, updatable = false, length = 24)
    private SalesChannel originChannel;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_channel", nullable = false, length = 24)
    private SalesChannel lastChannel;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "cart_items", joinColumns = @JoinColumn(name = "cart_id"))
    private List<CartItem> items = new ArrayList<>();

    @Column(name = "idempotency_key", nullable = false, updatable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "correlation_id", nullable = false, updatable = false)
    private UUID correlationId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Cart() {
    }

    private Cart(UUID id, UUID customerId, SalesChannel channel, String idempotencyKey,
                 UUID correlationId, Instant now) {
        this.id = id;
        this.customerId = customerId;
        this.status = CartStatus.ACTIVE;
        this.originChannel = channel;
        this.lastChannel = channel;
        this.idempotencyKey = idempotencyKey;
        this.correlationId = correlationId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Cart open(UUID customerId, SalesChannel channel, String idempotencyKey,
                            UUID correlationId, Instant now) {
        return new Cart(UUID.randomUUID(), customerId, channel, idempotencyKey, correlationId, now);
    }

    void recoverFrom(SalesChannel channel, Instant now) {
        requireActive();
        this.lastChannel = channel;
        this.updatedAt = now;
    }

    void putItem(UUID productId, String productName, BigDecimal unitPrice, int quantity, Instant now) {
        requireActive();
        items.stream().filter(item -> item.getProductId().equals(productId)).findFirst()
                .ifPresentOrElse(
                        item -> item.update(productName, unitPrice, quantity),
                        () -> items.add(CartItem.of(productId, productName, unitPrice, quantity)));
        this.updatedAt = now;
    }

    void removeItem(UUID productId, Instant now) {
        requireActive();
        boolean removed = items.removeIf(item -> item.getProductId().equals(productId));
        if (!removed) {
            throw new CartItemNotFoundException(id, productId);
        }
        this.updatedAt = now;
    }

    void checkOut(Instant now) {
        requireActive();
        if (items.isEmpty()) {
            throw new EmptyCartException(id);
        }
        status = CartStatus.CHECKED_OUT;
        updatedAt = now;
    }

    void abandon(Instant now) {
        requireActive();
        status = CartStatus.ABANDONED;
        updatedAt = now;
    }

    private void requireActive() {
        if (status != CartStatus.ACTIVE) {
            throw new CartNotActiveException(id, status);
        }
    }

    public BigDecimal total() {
        return items.stream().map(CartItem::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int itemCount() {
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }

    public UUID getId() { return id; }
    public UUID getCustomerId() { return customerId; }
    public CartStatus getStatus() { return status; }
    public SalesChannel getOriginChannel() { return originChannel; }
    public SalesChannel getLastChannel() { return lastChannel; }
    public List<CartItem> getItems() { return List.copyOf(items); }
    public String getIdempotencyKey() { return idempotencyKey; }
    public UUID getCorrelationId() { return correlationId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
