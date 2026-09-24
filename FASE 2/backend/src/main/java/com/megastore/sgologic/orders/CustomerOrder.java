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
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Orden y máquina de estados de ORD-02. */
@Entity
@Table(name = "customer_orders")
public class CustomerOrder {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = transitions();

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "cart_id", nullable = false, updatable = false, unique = true)
    private UUID cartId;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, updatable = false, length = 24)
    private SalesChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private OrderStatus status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "customer_order_items", joinColumns = @JoinColumn(name = "order_id"))
    private List<OrderLine> items = new ArrayList<>();

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "checkout_key", nullable = false, updatable = false, unique = true, length = 128)
    private String checkoutKey;

    @Column(name = "correlation_id", nullable = false, updatable = false)
    private UUID correlationId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected CustomerOrder() {
    }

    private CustomerOrder(UUID id, Cart cart, String checkoutKey, UUID correlationId, Instant now) {
        this.id = id;
        this.cartId = cart.getId();
        this.customerId = cart.getCustomerId();
        this.channel = cart.getLastChannel();
        this.status = OrderStatus.CREATED;
        this.items = cart.getItems().stream().map(OrderLine::from).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        this.total = cart.total();
        this.checkoutKey = checkoutKey;
        this.correlationId = correlationId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    static CustomerOrder create(Cart cart, String checkoutKey, UUID correlationId, Instant now) {
        return new CustomerOrder(UUID.randomUUID(), cart, checkoutKey, correlationId, now);
    }

    void transitionTo(OrderStatus next, Instant now) {
        Set<OrderStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(status, Set.of());
        if (!allowed.contains(next)) {
            throw new InvalidOrderTransitionException(id, status, next);
        }
        status = next;
        updatedAt = now;
    }

    private static Map<OrderStatus, Set<OrderStatus>> transitions() {
        Map<OrderStatus, Set<OrderStatus>> map = new EnumMap<>(OrderStatus.class);
        map.put(OrderStatus.CREATED, EnumSet.of(OrderStatus.INVENTORY_RESERVED, OrderStatus.CANCELLED));
        map.put(OrderStatus.INVENTORY_RESERVED, EnumSet.of(OrderStatus.PAYMENT_PENDING, OrderStatus.CANCELLED));
        map.put(OrderStatus.PAYMENT_PENDING, EnumSet.of(OrderStatus.PAYMENT_APPROVED,
                OrderStatus.PAYMENT_REJECTED, OrderStatus.PAYMENT_INDETERMINATE));
        map.put(OrderStatus.PAYMENT_REJECTED, EnumSet.of(OrderStatus.PAYMENT_PENDING, OrderStatus.CANCELLED));
        map.put(OrderStatus.PAYMENT_INDETERMINATE,
                EnumSet.of(OrderStatus.PAYMENT_APPROVED, OrderStatus.PAYMENT_REJECTED));
        map.put(OrderStatus.PAYMENT_APPROVED, EnumSet.of(OrderStatus.FISCAL_PENDING));
        map.put(OrderStatus.FISCAL_PENDING,
                EnumSet.of(OrderStatus.FISCAL_APPROVED, OrderStatus.FISCAL_REJECTED));
        map.put(OrderStatus.FISCAL_REJECTED, EnumSet.of(OrderStatus.FISCAL_PENDING));
        map.put(OrderStatus.FISCAL_APPROVED, EnumSet.of(OrderStatus.CONFIRMED));
        map.put(OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.PREPARING));
        map.put(OrderStatus.PREPARING, EnumSet.of(OrderStatus.IN_TRANSIT));
        map.put(OrderStatus.IN_TRANSIT, EnumSet.of(OrderStatus.DELIVERED));
        return Map.copyOf(map);
    }

    public UUID getId() { return id; }
    public UUID getCartId() { return cartId; }
    public UUID getCustomerId() { return customerId; }
    public SalesChannel getChannel() { return channel; }
    public OrderStatus getStatus() { return status; }
    public List<OrderLine> getItems() { return List.copyOf(items); }
    public BigDecimal getTotal() { return total; }
    public String getCheckoutKey() { return checkoutKey; }
    public UUID getCorrelationId() { return correlationId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
