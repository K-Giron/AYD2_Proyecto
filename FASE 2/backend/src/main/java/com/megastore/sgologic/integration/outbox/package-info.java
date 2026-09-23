/**
 * API pública y mínima del Outbox/Inbox transversal (BB-04).
 *
 * <p>Solo este subpaquete puede ser usado por otros módulos de negocio:
 * {@link com.megastore.sgologic.integration.outbox.PublishEvent} para
 * registrar un evento en la misma transacción del cambio de negocio, e
 * {@link com.megastore.sgologic.integration.outbox.IdempotentConsumer} para
 * deduplicar consumidores. El resto del módulo {@code integration} es
 * interno.</p>
 */
@org.springframework.modulith.NamedInterface("outbox")
package com.megastore.sgologic.integration.outbox;
