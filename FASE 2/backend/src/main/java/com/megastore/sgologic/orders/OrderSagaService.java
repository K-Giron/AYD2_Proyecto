package com.megastore.sgologic.orders;

import com.megastore.sgologic.fiscal.FiscalProcessingResult;
import com.megastore.sgologic.fiscal.FiscalStatus;
import com.megastore.sgologic.inventory.InventoryLocationType;
import com.megastore.sgologic.inventory.InventoryReservationAllocation;
import com.megastore.sgologic.logistics.api.DispatchPort;
import com.megastore.sgologic.logistics.api.DispatchRequest;
import com.megastore.sgologic.logistics.api.DispatchResult;
import com.megastore.sgologic.payments.api.PaymentPort;
import com.megastore.sgologic.payments.api.PaymentResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ORD-03: coordinador reentrante del camino feliz. Cada módulo conserva sus
 * datos y transacciones; repetir la misma ejecución reutiliza sus claves de
 * idempotencia y continúa desde el estado persistido de la orden.
 */
@Service
public class OrderSagaService {

    private final OrdersService orders;
    private final PaymentPort payments;
    private final com.megastore.sgologic.fiscal.FiscalProcessingPort fiscal;
    private final DispatchPort dispatch;

    public OrderSagaService(OrdersService orders, PaymentPort payments,
                            com.megastore.sgologic.fiscal.FiscalProcessingPort fiscal,
                            DispatchPort dispatch) {
        this.orders = orders;
        this.payments = payments;
        this.fiscal = fiscal;
        this.dispatch = dispatch;
    }

    public OrderSagaResponse run(UUID orderId, RunOrderSagaRequest request, String sagaKey,
                                 String actor, UUID correlationId) {
        CustomerOrder order = orders.getOrder(orderId);
        PaymentResult payment = payments.latestForOrder(orderId).orElse(null);
        FiscalProcessingResult document = fiscal.latestForOrder(orderId).orElse(null);
        DispatchResult shipment = dispatch.latestForOrder(orderId).orElse(null);

        if (order.getStatus() == OrderStatus.CREATED) {
            orders.reserveInventory(orderId, sagaKey + ":inventory", actor, correlationId);
            order = orders.getOrder(orderId);
        }
        if (order.getStatus() == OrderStatus.INVENTORY_RESERVED) {
            OrderPaymentOutcome outcome = orders.processPayment(orderId, request.paymentMethod(),
                    request.currency(), request.paymentReference(), sagaKey + ":payment", actor, correlationId);
            payment = outcome.payment();
            order = outcome.order();
        }
        if (order.getStatus() == OrderStatus.PAYMENT_REJECTED) {
            order = orders.cancelOrder(orderId, "SAGA", "Pago rechazado; reserva liberada", correlationId);
            return response(order, payment, document, shipment, "COMPENSATED", "Pago rechazado sin perder stock.");
        }
        if (order.getStatus() == OrderStatus.PAYMENT_INDETERMINATE
                || order.getStatus() == OrderStatus.PAYMENT_PENDING) {
            return response(order, payment, document, shipment, "WAITING_PAYMENT",
                    "El intento se conserva y la conciliación resolverá el resultado.");
        }
        if (order.getStatus() == OrderStatus.PAYMENT_APPROVED) {
            payment = payment != null ? payment : payments.latestForOrder(orderId)
                    .orElseThrow(() -> new IllegalStateException("No existe intento aprobado para la orden."));
            OrderFiscalOutcome outcome = orders.requestFiscalDocument(orderId, payment.paymentAttemptId(),
                    request.taxIdentifier(), request.customerName(), request.currency(), sagaKey + ":fiscal",
                    actor, correlationId);
            document = outcome.fiscalDocument();
            order = outcome.order();
        }
        if (order.getStatus() == OrderStatus.FISCAL_PENDING) {
            document = fiscal.latestForOrder(orderId).orElse(document);
            if (document == null || document.status() == FiscalStatus.FISCAL_PENDING) {
                return response(order, payment, document, shipment, "WAITING_FISCAL",
                        "SAT continúa procesando; repita la misma solicitud para reanudar.");
            }
            OrderFiscalOutcome synchronizedFiscal = orders.synchronizeFiscalStatus(orderId, actor, correlationId);
            document = synchronizedFiscal.fiscalDocument();
            order = synchronizedFiscal.order();
        }
        if (order.getStatus() == OrderStatus.FISCAL_REJECTED) {
            return response(order, payment, document, shipment, "FISCAL_REJECTED",
                    "La venta no se confirma hasta resolver el rechazo fiscal.");
        }
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            List<InventoryReservationAllocation> allocations = orders.getInventoryReservations(orderId);
            if (allocations.isEmpty()) {
                throw new IllegalStateException("La orden confirmada no tiene asignación de inventario.");
            }
            orders.confirmInventoryReservations(orderId);
            InventoryReservationAllocation origin = allocations.getFirst();
            shipment = dispatch.createDispatch(new DispatchRequest(orderId,
                    origin.locationType() == InventoryLocationType.DISTRIBUTION_CENTER
                            ? "DISTRIBUTION_CENTER" : "STORE",
                    origin.locationCode(), request.destination(), correlationId, sagaKey + ":dispatch"));
            order = orders.transition(orderId, OrderStatus.PREPARING, "SAGA",
                    "Despacho asignado desde " + origin.locationCode(), correlationId);
        }
        if (order.getStatus() == OrderStatus.PREPARING) {
            shipment = shipment != null ? shipment : dispatch.latestForOrder(orderId).orElse(null);
            return response(order, payment, document, shipment, "COMPLETED",
                    "Compra confirmada y despacho iniciado.");
        }
        return response(order, payment, document, shipment, "NO_ACTION",
                "La Saga no modifica el estado actual " + order.getStatus() + ".");
    }

    private OrderSagaResponse response(CustomerOrder order, PaymentResult payment,
                                       FiscalProcessingResult fiscal, DispatchResult shipment,
                                       String sagaStatus, String detail) {
        UUID reservationId = orders.getInventoryReservations(order.getId()).stream()
                .findFirst().map(InventoryReservationAllocation::reservationId).orElse(null);
        return new OrderSagaResponse(order.getId(), order.getStatus(), sagaStatus,
                payment == null ? null : payment.paymentAttemptId(),
                fiscal == null ? null : fiscal.documentId(),
                fiscal == null ? null : fiscal.cui(),
                fiscal == null ? null : fiscal.series(),
                reservationId,
                shipment == null ? null : shipment.shipmentId(), detail);
    }
}
