package com.megastore.sgologic.payments.api;

import java.util.Optional;
import java.util.UUID;

/**
 * Capacidad "autorización de pago" que la Saga (ORD-03) consume
 * (PAY-01, CDU-03). El módulo Payments conserva la propiedad de sus datos y
 * del adaptador PayKlik.
 *
 * <p>Es idempotente por {@code idempotencyKey}: una repetición devuelve el
 * intento existente sin volver a llamar al proveedor. Un timeout o una caída
 * dejan el intento en {@code INDETERMINATE} y nunca se interpretan como
 * aprobación (ADR-003); la conciliación de PAY-03 lo resuelve.</p>
 *
 * <p><b>Concurrencia:</b> si dos solicitudes simultáneas usan la misma
 * {@code idempotencyKey}, la segunda puede recibir el intento todavía en
 * {@code PENDING} mientras la primera sigue en vuelo. El consumidor (Saga)
 * debe consultar el estado o esperar su resolución; la conciliación lo
 * corrige si el proceso cae.</p>
 */
public interface PaymentPort {

    PaymentResult requestPayment(PaymentRequest request);

    /** Último intento observable para que una Saga interrumpida pueda reanudarse. */
    Optional<PaymentResult> latestForOrder(UUID orderId);
}
