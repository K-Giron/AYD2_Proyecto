package com.megastore.sgologic.payments.api;

/**
 * Capacidad "autorización de pago" que la Saga (ORD-03) consume
 * (PAY-01, CDU-03). El módulo Payments conserva la propiedad de sus datos y
 * del adaptador PayKlik.
 *
 * <p>Es idempotente por {@code idempotencyKey}: una repetición devuelve el
 * intento existente sin volver a llamar al proveedor. Un timeout o una caída
 * dejan el intento en {@code PENDING} y nunca se interpretan como aprobación
 * (ADR-003); el estado indeterminado y la conciliación pertenecen a PAY-02 y
 * PAY-03.</p>
 */
public interface PaymentPort {

    PaymentResult requestPayment(PaymentRequest request);
}
