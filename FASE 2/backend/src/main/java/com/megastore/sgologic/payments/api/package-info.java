/**
 * API pública del módulo Payments para la Saga (PAY-01).
 *
 * <p>{@link com.megastore.sgologic.payments.api.PaymentPort} materializa la
 * capacidad "autorización de pago" descrita en
 * {@code docs/contracts/orders/saga-required-ports.md}: recibe orden, importe,
 * referencia segura, correlación e idempotencia, y devuelve el intento, su
 * resultado y el instante. El resto del módulo {@code payments} (entidad,
 * repositorio y adaptador PayKlik) es interno.</p>
 */
@org.springframework.modulith.NamedInterface("payments-api")
package com.megastore.sgologic.payments.api;
