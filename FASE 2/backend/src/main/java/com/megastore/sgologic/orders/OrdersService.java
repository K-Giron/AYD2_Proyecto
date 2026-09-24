package com.megastore.sgologic.orders;

import com.megastore.sgologic.audit.AuditCommand;
import com.megastore.sgologic.audit.AuditResult;
import com.megastore.sgologic.audit.AuditService;
import com.megastore.sgologic.fiscal.FiscalProcessingPort;
import com.megastore.sgologic.fiscal.FiscalProcessingRequest;
import com.megastore.sgologic.fiscal.FiscalProcessingResult;
import com.megastore.sgologic.fiscal.FiscalStatus;
import com.megastore.sgologic.inventory.InventoryReservationAllocation;
import com.megastore.sgologic.inventory.InventoryReservationLine;
import com.megastore.sgologic.inventory.InventoryReservationPort;
import com.megastore.sgologic.messaging.OutboxCommand;
import com.megastore.sgologic.messaging.OutboxService;
import com.megastore.sgologic.payments.PaymentMethod;
import com.megastore.sgologic.payments.PaymentProcessingPort;
import com.megastore.sgologic.payments.PaymentProcessingRequest;
import com.megastore.sgologic.payments.PaymentProcessingResult;
import com.megastore.sgologic.payments.PaymentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Casos de aplicación de ORD-01 y base ejecutable de ORD-02. */
@Service
public class OrdersService {

    private final CartRepository cartRepository;
    private final CustomerOrderRepository orderRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final InventoryReservationPort inventoryReservationPort;
    private final PaymentProcessingPort paymentProcessingPort;
    private final FiscalProcessingPort fiscalProcessingPort;
    private final OutboxService outboxService;
    private final AuditService auditService;
    private final Clock clock;

    public OrdersService(CartRepository cartRepository,
                         CustomerOrderRepository orderRepository,
                         OrderStatusHistoryRepository historyRepository,
                         InventoryReservationPort inventoryReservationPort,
                         PaymentProcessingPort paymentProcessingPort,
                         FiscalProcessingPort fiscalProcessingPort,
                         OutboxService outboxService,
                         AuditService auditService,
                         Clock clock) {
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.historyRepository = historyRepository;
        this.inventoryReservationPort = inventoryReservationPort;
        this.paymentProcessingPort = paymentProcessingPort;
        this.fiscalProcessingPort = fiscalProcessingPort;
        this.outboxService = outboxService;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public Cart openOrRecoverCart(UUID customerId, SalesChannel channel,
                                  String idempotencyKey, UUID correlationId) {
        Optional<Cart> repeated = cartRepository.findByIdempotencyKey(idempotencyKey);
        if (repeated.isPresent()) {
            return repeated.get();
        }

        Instant now = clock.instant();
        return cartRepository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE)
                .map(cart -> {
                    cart.recoverFrom(channel, now);
                    return cartRepository.save(cart);
                })
                .orElseGet(() -> cartRepository.save(
                        Cart.open(customerId, channel, idempotencyKey, correlationId, now)));
    }

    @Transactional
    public Cart putItem(UUID cartId, UUID productId, String productName,
                        BigDecimal unitPrice, int quantity) {
        Cart cart = getCart(cartId);
        cart.putItem(productId, productName, unitPrice, quantity, clock.instant());
        return cartRepository.save(cart);
    }

    @Transactional
    public Cart removeItem(UUID cartId, UUID productId) {
        Cart cart = getCart(cartId);
        cart.removeItem(productId, clock.instant());
        return cartRepository.save(cart);
    }

    @Transactional
    public Cart abandonCart(UUID cartId) {
        Cart cart = getCart(cartId);
        cart.abandon(clock.instant());
        return cartRepository.save(cart);
    }

    public Cart getCart(UUID cartId) {
        return cartRepository.findById(cartId).orElseThrow(() -> new CartNotFoundException(cartId));
    }

    public Cart getActiveCart(UUID customerId) {
        return cartRepository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE)
                .orElseThrow(() -> new ActiveCartNotFoundException(customerId));
    }

    @Transactional
    public CustomerOrder checkOut(UUID cartId, String checkoutKey, UUID correlationId, String actor) {
        Optional<CustomerOrder> repeated = orderRepository.findByCheckoutKey(checkoutKey);
        if (repeated.isPresent()) {
            return repeated.get();
        }
        Optional<CustomerOrder> existingForCart = orderRepository.findByCartId(cartId);
        if (existingForCart.isPresent()) {
            return existingForCart.get();
        }

        Instant now = clock.instant();
        Cart cart = getCart(cartId);
        cart.checkOut(now);
        CustomerOrder order = orderRepository.save(
                CustomerOrder.create(cart, checkoutKey, correlationId, now));
        cartRepository.save(cart);
        recordTransition(order.getId(), null, OrderStatus.CREATED, actor,
                "Orden creada desde carrito omnicanal", correlationId, now, AuditResult.SUCCESS);
        return order;
    }

    @Transactional(noRollbackFor = InvalidOrderTransitionException.class)
    public CustomerOrder transition(UUID orderId, OrderStatus next, String actor,
                                    String reason, UUID correlationId) {
        CustomerOrder order = getOrder(orderId);
        OrderStatus previous = order.getStatus();
        Instant now = clock.instant();
        try {
            order.transitionTo(next, now);
        } catch (InvalidOrderTransitionException exception) {
            recordTransition(orderId, previous, previous, actor,
                    "Transición rechazada a " + next + ": " + reason,
                    correlationId, now, AuditResult.REJECTED);
            throw exception;
        }
        CustomerOrder saved = orderRepository.save(order);
        recordTransition(orderId, previous, next, actor, reason,
                correlationId, now, AuditResult.SUCCESS);
        return saved;
    }

    public CustomerOrder getOrder(UUID orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    public List<CustomerOrder> getOrdersForCustomer(UUID customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    public List<CustomerOrder> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<OrderStatusHistory> getAuditHistory(UUID orderId) {
        getOrder(orderId);
        return historyRepository.findByOrderIdOrderByOccurredAtAsc(orderId);
    }

    @Transactional
    public InventoryReservationOutcome reserveInventory(UUID orderId, String idempotencyKey,
                                                         String actor, UUID correlationId) {
        CustomerOrder order = getOrder(orderId);
        if (order.getStatus() == OrderStatus.INVENTORY_RESERVED) {
            throw new InvalidOrderTransitionException(orderId, order.getStatus(), OrderStatus.INVENTORY_RESERVED);
        }
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new InvalidOrderTransitionException(orderId, order.getStatus(), OrderStatus.INVENTORY_RESERVED);
        }

        List<InventoryReservationAllocation> allocations = inventoryReservationPort.reserveOrder(
                orderId,
                order.getItems().stream()
                        .map(line -> new InventoryReservationLine(line.getProductId(), line.getQuantity()))
                        .toList(),
                correlationId,
                idempotencyKey);

        OrderStatus previous = order.getStatus();
        Instant now = clock.instant();
        order.transitionTo(OrderStatus.INVENTORY_RESERVED, now);
        orderRepository.save(order);
        recordTransition(orderId, previous, OrderStatus.INVENTORY_RESERVED,
                actor, "Inventario reservado para todas las líneas", correlationId, now, AuditResult.SUCCESS);
        return new InventoryReservationOutcome(order, allocations);
    }

    @Transactional
    public OrderPaymentOutcome processPayment(UUID orderId, PaymentMethod method, String currency,
                                               String paymentReference, String idempotencyKey,
                                               String actor, UUID correlationId) {
        CustomerOrder order = getOrder(orderId);
        if (order.getStatus() != OrderStatus.INVENTORY_RESERVED) {
            PaymentProcessingResult repeated = paymentProcessingPort.findByIdempotencyKey(idempotencyKey)
                    .filter(result -> result.orderId().equals(orderId))
                    .orElseThrow(() -> new InvalidOrderTransitionException(
                            orderId, order.getStatus(), OrderStatus.PAYMENT_PENDING));
            return new OrderPaymentOutcome(order, repeated);
        }

        Instant now = clock.instant();
        order.transitionTo(OrderStatus.PAYMENT_PENDING, now);
        orderRepository.save(order);
        recordTransition(orderId, OrderStatus.INVENTORY_RESERVED, OrderStatus.PAYMENT_PENDING,
                actor, "Intento de pago persistido", correlationId, now, AuditResult.SUCCESS);

        PaymentProcessingResult result = paymentProcessingPort.process(new PaymentProcessingRequest(
                orderId, order.getChannel().name(), method, order.getTotal(), currency,
                paymentReference, correlationId, idempotencyKey));
        OrderStatus next = switch (result.status()) {
            case APPROVED -> OrderStatus.PAYMENT_APPROVED;
            case REJECTED -> OrderStatus.PAYMENT_REJECTED;
            case INDETERMINATE, PENDING -> OrderStatus.PAYMENT_INDETERMINATE;
        };
        order.transitionTo(next, clock.instant());
        orderRepository.save(order);
        recordTransition(orderId, OrderStatus.PAYMENT_PENDING, next, actor,
                paymentReason(result.status()), correlationId, clock.instant(), AuditResult.SUCCESS);
        return new OrderPaymentOutcome(order, result);
    }

    private String paymentReason(PaymentStatus status) {
        return switch (status) {
            case APPROVED -> "Pago aprobado por PayKlik";
            case REJECTED -> "Pago rechazado por PayKlik";
            case INDETERMINATE, PENDING -> "Pago pendiente de conciliación con PayKlik";
        };
    }

    @Transactional
    public OrderFiscalOutcome requestFiscalDocument(UUID orderId, UUID paymentAttemptId,
                                                     String taxIdentifier, String customerName,
                                                     String currency, String idempotencyKey,
                                                     String actor, UUID correlationId) {
        CustomerOrder order = getOrder(orderId);
        if (order.getStatus() != OrderStatus.PAYMENT_APPROVED) {
            throw new InvalidOrderTransitionException(orderId, order.getStatus(), OrderStatus.FISCAL_PENDING);
        }
        order.transitionTo(OrderStatus.FISCAL_PENDING, clock.instant());
        orderRepository.save(order);
        recordTransition(orderId, OrderStatus.PAYMENT_APPROVED, OrderStatus.FISCAL_PENDING,
                actor, "Documento fiscal enviado a SAT", correlationId, clock.instant(), AuditResult.SUCCESS);
        FiscalProcessingResult result = fiscalProcessingPort.requestInvoice(new FiscalProcessingRequest(
                orderId, paymentAttemptId, taxIdentifier, customerName, order.getTotal(), currency,
                correlationId, idempotencyKey));
        return new OrderFiscalOutcome(order, result);
    }

    @Transactional
    public OrderFiscalOutcome synchronizeFiscalStatus(UUID orderId, String actor, UUID correlationId) {
        CustomerOrder order = getOrder(orderId);
        if (order.getStatus() != OrderStatus.FISCAL_PENDING) {
            throw new InvalidOrderTransitionException(orderId, order.getStatus(), OrderStatus.FISCAL_APPROVED);
        }
        FiscalProcessingResult fiscal = fiscalProcessingPort.latestForOrder(orderId)
                .orElseThrow(() -> new IllegalStateException("La orden no tiene documento fiscal asociado."));
        if (fiscal.status() == FiscalStatus.FISCAL_PENDING) return new OrderFiscalOutcome(order, fiscal);

        OrderStatus fiscalState = fiscal.status() == FiscalStatus.APPROVED
                ? OrderStatus.FISCAL_APPROVED : OrderStatus.FISCAL_REJECTED;
        order.transitionTo(fiscalState, clock.instant());
        recordTransition(orderId, OrderStatus.FISCAL_PENDING, fiscalState,
                actor, "Resultado recibido de SAT", correlationId, clock.instant(), AuditResult.SUCCESS);
        if (fiscalState == OrderStatus.FISCAL_APPROVED) {
            order.transitionTo(OrderStatus.CONFIRMED, clock.instant());
            recordTransition(orderId, OrderStatus.FISCAL_APPROVED, OrderStatus.CONFIRMED,
                    actor, "Orden confirmada con evidencia fiscal", correlationId,
                    clock.instant(), AuditResult.SUCCESS);
        }
        orderRepository.save(order);
        return new OrderFiscalOutcome(order, fiscal);
    }

    private void recordTransition(UUID orderId, OrderStatus previous, OrderStatus next,
                                  String actor, String reason, UUID correlationId,
                                  Instant occurredAt, AuditResult result) {
        historyRepository.save(OrderStatusHistory.of(orderId, previous, next, actor,
                reason, correlationId, occurredAt));

        Map<String, Object> previousState = previous == null ? Map.of() : Map.of("status", previous.name());
        Map<String, Object> newState = Map.of("status", next.name());
        String action = result == AuditResult.REJECTED
                ? "ORDER_TRANSITION_REJECTED" : "ORDER_STATUS_CHANGED";

        auditService.record(new AuditCommand(actor, actorType(actor), action,
                "ORDER", orderId, orderId, correlationId, correlationId,
                previousState, newState, result, Map.of("reason", reason)));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", orderId);
        orderRepository.findById(orderId)
                .map(CustomerOrder::getCustomerId)
                .ifPresent(customerId -> payload.put("customerId", customerId));
        payload.put("previousStatus", previous == null ? null : previous.name());
        payload.put("newStatus", next.name());
        payload.put("actor", actor);
        payload.put("reason", reason);
        payload.put("result", result.name());
        payload.put("occurredAt", occurredAt.toString());
        outboxService.record(new OutboxCommand("ORDER", orderId, action, payload,
                correlationId, correlationId + ":" + action + ":" + occurredAt));
    }

    private String actorType(String actor) {
        if (actor == null || actor.isBlank()) return "SYSTEM";
        int separator = actor.indexOf(':');
        return (separator > 0 ? actor.substring(0, separator) : actor).toUpperCase();
    }
}
