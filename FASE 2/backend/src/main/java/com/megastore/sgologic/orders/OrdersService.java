package com.megastore.sgologic.orders;

import com.megastore.sgologic.audit.api.AuditEntryRequest;
import com.megastore.sgologic.audit.api.AuditRecorder;
import com.megastore.sgologic.credit.api.CreditOperationResult;
import com.megastore.sgologic.credit.api.CreditOperationStatus;
import com.megastore.sgologic.credit.api.CreditPort;
import com.megastore.sgologic.credit.api.CreditRequest;
import com.megastore.sgologic.fiscal.FinancialOperationType;
import com.megastore.sgologic.fiscal.FiscalProcessingPort;
import com.megastore.sgologic.fiscal.FiscalProcessingRequest;
import com.megastore.sgologic.fiscal.FiscalProcessingResult;
import com.megastore.sgologic.fiscal.FiscalStatus;
import com.megastore.sgologic.inventory.InventoryReservationAllocation;
import com.megastore.sgologic.inventory.InventoryReservationLine;
import com.megastore.sgologic.inventory.InventoryReservationPort;
import com.megastore.sgologic.integration.outbox.OutboxEventRequest;
import com.megastore.sgologic.integration.outbox.PublishEvent;
import com.megastore.sgologic.payments.api.PaymentMethod;
import com.megastore.sgologic.payments.api.PaymentPort;
import com.megastore.sgologic.payments.api.PaymentRequest;
import com.megastore.sgologic.payments.api.PaymentResult;
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
    private final PaymentPort paymentPort;
    private final CreditPort creditPort;
    private final FiscalProcessingPort fiscalProcessingPort;
    private final PublishEvent eventPublisher;
    private final AuditRecorder auditRecorder;
    private final Clock clock;

    public OrdersService(CartRepository cartRepository,
                         CustomerOrderRepository orderRepository,
                         OrderStatusHistoryRepository historyRepository,
                         InventoryReservationPort inventoryReservationPort,
                         PaymentPort paymentPort,
                         CreditPort creditPort,
                         FiscalProcessingPort fiscalProcessingPort,
                         PublishEvent eventPublisher,
                         AuditRecorder auditRecorder,
                         Clock clock) {
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.historyRepository = historyRepository;
        this.inventoryReservationPort = inventoryReservationPort;
        this.paymentPort = paymentPort;
        this.creditPort = creditPort;
        this.fiscalProcessingPort = fiscalProcessingPort;
        this.eventPublisher = eventPublisher;
        this.auditRecorder = auditRecorder;
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
                "Orden creada desde carrito omnicanal", correlationId, now, "SUCCESS");
        return order;
    }

    @Transactional(noRollbackFor = InvalidOrderTransitionException.class)
    public CustomerOrder transition(UUID orderId, OrderStatus next, String actor,
                                    String reason, UUID correlationId) {
        if (next == OrderStatus.CANCELLED) {
            return cancelOrder(orderId, actor, reason, correlationId);
        }
        CustomerOrder order = getOrder(orderId);
        OrderStatus previous = order.getStatus();
        Instant now = clock.instant();
        try {
            order.transitionTo(next, now);
        } catch (InvalidOrderTransitionException exception) {
            recordTransition(orderId, previous, previous, actor,
                    "Transición rechazada a " + next + ": " + reason,
                    correlationId, now, "REJECTED");
            throw exception;
        }
        CustomerOrder saved = orderRepository.save(order);
        recordTransition(orderId, previous, next, actor, reason,
                correlationId, now, "SUCCESS");
        return saved;
    }

    /**
     * ORD-04: cancela únicamente estados compensables sin cobro aprobado y
     * libera las reservas activas en la misma unidad de trabajo.
     */
    @Transactional
    public CustomerOrder cancelOrder(UUID orderId, String actor, String reason, UUID correlationId) {
        CustomerOrder order = getOrder(orderId);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return order;
        }
        OrderStatus previous = order.getStatus();
        if (previous != OrderStatus.CREATED
                && previous != OrderStatus.INVENTORY_RESERVED
                && previous != OrderStatus.PAYMENT_REJECTED
                && previous != OrderStatus.CREDIT_REJECTED) {
            throw new InvalidOrderTransitionException(orderId, previous, OrderStatus.CANCELLED);
        }
        inventoryReservationPort.releaseOrderReservations(orderId);
        order.transitionTo(OrderStatus.CANCELLED, clock.instant());
        CustomerOrder saved = orderRepository.save(order);
        recordTransition(orderId, previous, OrderStatus.CANCELLED, actor,
                reason == null || reason.isBlank() ? "Orden cancelada y reservas compensadas" : reason,
                correlationId, clock.instant(), "SUCCESS");
        return saved;
    }

    /** Confirma el consumo del stock una vez satisfecha la compuerta fiscal. */
    @Transactional
    public void confirmInventoryReservations(UUID orderId) {
        getOrder(orderId);
        inventoryReservationPort.confirmOrderReservations(orderId);
    }

    public List<InventoryReservationAllocation> getInventoryReservations(UUID orderId) {
        getOrder(orderId);
        return inventoryReservationPort.findOrderReservations(orderId);
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
                actor, "Inventario reservado para todas las líneas", correlationId, now, "SUCCESS");
        return new InventoryReservationOutcome(order, allocations);
    }

    @Transactional
    public OrderPaymentOutcome processPayment(UUID orderId, PaymentMethod method, String currency,
                                               String paymentReference, String idempotencyKey,
                                               String actor, UUID correlationId) {
        CustomerOrder order = getOrder(orderId);
        if (order.getStatus() != OrderStatus.INVENTORY_RESERVED
                && order.getStatus() != OrderStatus.CREDIT_REJECTED) {
            if (order.getPaymentRoute() != PaymentRoute.PAYKLIK) {
                throw new InvalidOrderTransitionException(
                        orderId, order.getStatus(), OrderStatus.PAYMENT_PENDING);
            }
            PaymentResult repeated = paymentPort.requestPayment(new PaymentRequest(
                    orderId, order.getTotal(), currency, order.getChannel().name(), method.name(),
                    paymentReference, correlationId, idempotencyKey));
            return new OrderPaymentOutcome(order, repeated);
        }

        OrderStatus previous = order.getStatus();
        order.selectPaymentRoute(PaymentRoute.PAYKLIK);
        Instant now = clock.instant();
        order.transitionTo(OrderStatus.PAYMENT_PENDING, now);
        orderRepository.save(order);
        recordTransition(orderId, previous, OrderStatus.PAYMENT_PENDING,
                actor, "Intento de pago persistido", correlationId, now, "SUCCESS");

        PaymentResult result = paymentPort.requestPayment(new PaymentRequest(
                orderId, order.getTotal(), currency, order.getChannel().name(), method.name(),
                paymentReference, correlationId, idempotencyKey));
        OrderStatus next = switch (result.status()) {
            case "APPROVED" -> OrderStatus.PAYMENT_APPROVED;
            case "REJECTED" -> OrderStatus.PAYMENT_REJECTED;
            case "INDETERMINATE", "PENDING" -> OrderStatus.PAYMENT_INDETERMINATE;
            default -> throw new IllegalStateException("Estado de pago no soportado: " + result.status());
        };
        order.transitionTo(next, clock.instant());
        order.recordFinancialOperation(result.paymentAttemptId());
        orderRepository.save(order);
        recordTransition(orderId, OrderStatus.PAYMENT_PENDING, next, actor,
                paymentReason(result.status()), correlationId, clock.instant(), "SUCCESS");
        return new OrderPaymentOutcome(order, result);
    }

    @Transactional
    public OrderCreditOutcome processCredit(UUID orderId, String currency, String idempotencyKey,
                                            String actor, UUID correlationId) {
        CustomerOrder order = getOrder(orderId);
        if (order.getStatus() != OrderStatus.INVENTORY_RESERVED) {
            CreditOperationResult repeated = creditPort.latestForOrder(orderId)
                    .orElseThrow(() -> new InvalidOrderTransitionException(
                            orderId, order.getStatus(), OrderStatus.CREDIT_PENDING));
            return new OrderCreditOutcome(order, repeated);
        }

        order.selectPaymentRoute(PaymentRoute.OWN_CREDIT);
        order.transitionTo(OrderStatus.CREDIT_PENDING, clock.instant());
        orderRepository.save(order);
        recordTransition(orderId, OrderStatus.INVENTORY_RESERVED, OrderStatus.CREDIT_PENDING,
                actor, "Solicitud de crédito propio persistida", correlationId, clock.instant(), "SUCCESS");

        CreditOperationResult result = creditPort.requestCredit(new CreditRequest(
                orderId, order.getCustomerId(), order.getTotal(), currency,
                correlationId, idempotencyKey));
        OrderStatus next = result.status() == CreditOperationStatus.GRANTED
                ? OrderStatus.CREDIT_APPROVED : OrderStatus.CREDIT_REJECTED;
        order.transitionTo(next, clock.instant());
        order.recordFinancialOperation(result.creditOperationId());
        orderRepository.save(order);
        recordTransition(orderId, OrderStatus.CREDIT_PENDING, next, actor,
                result.status() == CreditOperationStatus.GRANTED
                        ? "Crédito propio aprobado con vencimiento " + result.dueDate()
                        : "Crédito propio rechazado: " + result.publicReasonCode(),
                correlationId, clock.instant(), "SUCCESS");
        return new OrderCreditOutcome(order, result);
    }

    private String paymentReason(String status) {
        return switch (status) {
            case "APPROVED" -> "Pago aprobado por PayKlik";
            case "REJECTED" -> "Pago rechazado por PayKlik";
            case "INDETERMINATE", "PENDING" -> "Pago pendiente de conciliación con PayKlik";
            default -> "Resultado de pago " + status;
        };
    }

    @Transactional
    public OrderFiscalOutcome requestFiscalDocument(UUID orderId, UUID financialOperationId,
                                                     FinancialOperationType financialOperationType,
                                                     String taxIdentifier, String customerName,
                                                     String currency, String idempotencyKey,
                                                     String actor, UUID correlationId) {
        CustomerOrder order = getOrder(orderId);
        if (order.getStatus() != OrderStatus.PAYMENT_APPROVED
                && order.getStatus() != OrderStatus.CREDIT_APPROVED) {
            throw new InvalidOrderTransitionException(orderId, order.getStatus(), OrderStatus.FISCAL_PENDING);
        }
        order.transitionTo(OrderStatus.FISCAL_PENDING, clock.instant());
        orderRepository.save(order);
        OrderStatus previous = financialOperationType == FinancialOperationType.OWN_CREDIT
                ? OrderStatus.CREDIT_APPROVED : OrderStatus.PAYMENT_APPROVED;
        recordTransition(orderId, previous, OrderStatus.FISCAL_PENDING,
                actor, "Documento fiscal enviado a SAT", correlationId, clock.instant(), "SUCCESS");
        FiscalProcessingResult result = fiscalProcessingPort.requestInvoice(new FiscalProcessingRequest(
                orderId, financialOperationId, financialOperationType,
                taxIdentifier, customerName, order.getTotal(), currency,
                correlationId, idempotencyKey));
        return new OrderFiscalOutcome(order, result);
    }

    /** Compatibilidad del endpoint F2 dedicado a PayKlik. */
    public OrderFiscalOutcome requestFiscalDocument(UUID orderId, UUID paymentAttemptId,
                                                     String taxIdentifier, String customerName,
                                                     String currency, String idempotencyKey,
                                                     String actor, UUID correlationId) {
        return requestFiscalDocument(orderId, paymentAttemptId, FinancialOperationType.PAYKLIK,
                taxIdentifier, customerName, currency, idempotencyKey, actor, correlationId);
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
                actor, "Resultado recibido de SAT", correlationId, clock.instant(), "SUCCESS");
        if (fiscalState == OrderStatus.FISCAL_APPROVED) {
            order.transitionTo(OrderStatus.CONFIRMED, clock.instant());
            recordTransition(orderId, OrderStatus.FISCAL_APPROVED, OrderStatus.CONFIRMED,
                    actor, "Orden confirmada con evidencia fiscal", correlationId,
                    clock.instant(), "SUCCESS");
        }
        orderRepository.save(order);
        return new OrderFiscalOutcome(order, fiscal);
    }

    private void recordTransition(UUID orderId, OrderStatus previous, OrderStatus next,
                                  String actor, String reason, UUID correlationId,
                                  Instant occurredAt, String result) {
        historyRepository.save(OrderStatusHistory.of(orderId, previous, next, actor,
                reason, correlationId, occurredAt));

        Map<String, Object> previousState = previous == null ? Map.of() : Map.of("status", previous.name());
        Map<String, Object> newState = Map.of("status", next.name());
        String action = "REJECTED".equals(result)
                ? "ORDER_TRANSITION_REJECTED" : "ORDER_STATUS_CHANGED";

        auditRecorder.record(new AuditEntryRequest("ORDER", orderId, orderId, action,
                result, previous == null ? null : "{\"status\":\"" + previous.name() + "\"}",
                "{\"status\":\"" + next.name() + "\"}", correlationId, correlationId, null));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", orderId);
        orderRepository.findById(orderId).ifPresent(order -> {
            payload.put("customerId", order.getCustomerId());
            if (order.getPaymentRoute() != null) {
                payload.put("paymentRoute", order.getPaymentRoute().name());
            }
            if (order.getFinancialOperationId() != null) {
                payload.put("financialOperationId", order.getFinancialOperationId());
            }
        });
        payload.put("previousStatus", previous == null ? null : previous.name());
        payload.put("newStatus", next.name());
        payload.put("actor", actor);
        payload.put("reason", reason);
        payload.put("result", result);
        payload.put("occurredAt", occurredAt.toString());
        eventPublisher.publish(new OutboxEventRequest("ORDER", orderId, action, 1,
                correlationId, correlationId, payload));
    }

    private String actorType(String actor) {
        if (actor == null || actor.isBlank()) return "SYSTEM";
        int separator = actor.indexOf(':');
        return (separator > 0 ? actor.substring(0, separator) : actor).toUpperCase();
    }
}
