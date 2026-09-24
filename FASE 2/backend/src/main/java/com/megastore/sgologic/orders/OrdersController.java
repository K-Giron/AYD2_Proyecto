package com.megastore.sgologic.orders;

import com.megastore.sgologic.shared.http.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** API de ORD-01/ORD-02, alineada con CDU-01 y las convenciones v1. */
@RestController
@RequestMapping("/api/v1")
public class OrdersController {

    private final OrdersService service;

    public OrdersController(OrdersService service) {
        this.service = service;
    }

    @PostMapping("/carts")
    public ResponseEntity<CartResponse> openOrRecoverCart(
            @Valid @RequestBody OpenCartRequest request,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            HttpServletRequest httpRequest) {
        Cart cart = service.openOrRecoverCart(request.customerId(), request.channel(),
                idempotencyKey, correlationId(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(CartResponse.from(cart));
    }

    @GetMapping("/carts/{cartId}")
    public CartResponse getCart(@PathVariable UUID cartId) {
        return CartResponse.from(service.getCart(cartId));
    }

    @GetMapping("/carts/active")
    public CartResponse getActiveCart(@RequestParam UUID customerId) {
        return CartResponse.from(service.getActiveCart(customerId));
    }

    @PutMapping("/carts/{cartId}/items/{productId}")
    public CartResponse putItem(@PathVariable UUID cartId,
                                @PathVariable UUID productId,
                                @Valid @RequestBody PutCartItemRequest request) {
        return CartResponse.from(service.putItem(cartId, productId, request.productName(),
                request.unitPrice(), request.quantity()));
    }

    @DeleteMapping("/carts/{cartId}/items/{productId}")
    public CartResponse removeItem(@PathVariable UUID cartId, @PathVariable UUID productId) {
        return CartResponse.from(service.removeItem(cartId, productId));
    }

    @DeleteMapping("/carts/{cartId}")
    public CartResponse abandonCart(@PathVariable UUID cartId) {
        return CartResponse.from(service.abandonCart(cartId));
    }

    @PostMapping("/carts/{cartId}/checkout")
    public ResponseEntity<OrderResponse> checkOut(
            @PathVariable UUID cartId,
            @RequestHeader("Idempotency-Key") @NotBlank String checkoutKey,
            @RequestHeader(value = "X-Actor", defaultValue = "CUSTOMER") String actor,
            HttpServletRequest httpRequest) {
        CustomerOrder order = service.checkOut(cartId, checkoutKey, correlationId(httpRequest), actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(order));
    }

    @GetMapping("/orders/{orderId}")
    public OrderResponse getOrder(@PathVariable UUID orderId) {
        return OrderResponse.from(service.getOrder(orderId));
    }

    @GetMapping("/orders")
    public List<OrderResponse> getOrders(@RequestParam(required = false) UUID customerId) {
        List<CustomerOrder> orders = customerId == null
                ? service.getAllOrders() : service.getOrdersForCustomer(customerId);
        return orders.stream().map(OrderResponse::from).toList();
    }

    @GetMapping("/orders/{orderId}/audit-history")
    public List<OrderAuditEntryResponse> getAuditHistory(@PathVariable UUID orderId) {
        return service.getAuditHistory(orderId).stream().map(OrderAuditEntryResponse::from).toList();
    }

    @PatchMapping("/orders/{orderId}/status")
    public OrderResponse transition(
            @PathVariable UUID orderId,
            @Valid @RequestBody TransitionOrderRequest request,
            HttpServletRequest httpRequest) {
        return OrderResponse.from(service.transition(orderId, request.status(), request.actor(),
                request.reason(), correlationId(httpRequest)));
    }

    @PostMapping("/orders/{orderId}/inventory-reservations")
    public ReserveOrderResponse reserveInventory(
            @PathVariable UUID orderId,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @RequestHeader(value = "X-Actor", defaultValue = "SAGA") String actor,
            HttpServletRequest httpRequest) {
        return ReserveOrderResponse.from(service.reserveInventory(
                orderId, idempotencyKey, actor, correlationId(httpRequest)));
    }

    @PostMapping("/orders/{orderId}/payment-attempts")
    public OrderPaymentResponse processPayment(
            @PathVariable UUID orderId,
            @Valid @RequestBody ProcessPaymentRequest request,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @RequestHeader(value = "X-Actor", defaultValue = "CUSTOMER") String actor,
            HttpServletRequest httpRequest) {
        return OrderPaymentResponse.from(service.processPayment(orderId, request.method(), request.currency(),
                request.paymentReference(), idempotencyKey, actor, correlationId(httpRequest)));
    }

    @PostMapping("/orders/{orderId}/fiscal-documents")
    public ResponseEntity<OrderFiscalResponse> requestFiscalDocument(
            @PathVariable UUID orderId,
            @Valid @RequestBody RequestFiscalDocumentRequest request,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @RequestHeader(value = "X-Actor", defaultValue = "SAGA") String actor,
            HttpServletRequest httpRequest) {
        return ResponseEntity.accepted().body(OrderFiscalResponse.from(service.requestFiscalDocument(
                orderId, request.paymentAttemptId(), request.taxIdentifier(), request.customerName(),
                request.currency(), idempotencyKey, actor, correlationId(httpRequest))));
    }

    @PostMapping("/orders/{orderId}/fiscal-synchronization")
    public OrderFiscalResponse synchronizeFiscalStatus(
            @PathVariable UUID orderId,
            @RequestHeader(value = "X-Actor", defaultValue = "SAGA") String actor,
            HttpServletRequest httpRequest) {
        return OrderFiscalResponse.from(service.synchronizeFiscalStatus(
                orderId, actor, correlationId(httpRequest)));
    }

    private UUID correlationId(HttpServletRequest request) {
        return UUID.fromString(request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME).toString());
    }
}
