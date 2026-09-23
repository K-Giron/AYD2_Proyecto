# ORD-03 — Puertos requeridos por la Saga

**Estado:** `PROPOSED`

**Implementación:** parcial; Fiscal y Logistics tienen implementaciones locales, pero la Saga aún no está conectada

Este documento describe capacidades, no interfaces Java definitivas. Cada módulo conserva la propiedad de su implementación y sus datos.

| Puerto conceptual | Propietario proveedor | Entrada mínima propuesta | Resultado mínimo propuesto | Errores que deben distinguirse |
|---|---|---|---|---|
| Reserva de inventario | Jorge / Inventory | `orderId`, líneas, correlation ID, idempotency key | `reservationId`, origen, expiración, estado | Sin stock, conflicto, timeout, indisponible |
| Autorización de pago | Edson / Payments | `orderId`, importe, referencia segura, correlation ID, idempotency key | `paymentAttemptId`, resultado, instante | Rechazado, indeterminado, timeout, indisponible |
| Confirmación fiscal | Andy / Fiscal | `orderId`, pago confirmado, datos fiscales, correlation ID | `invoiceId`, CUI, serie, resultado | Rechazo, pendiente, timeout, indisponible |
| Solicitud de despacho | Kevin / Logistics | `orderId`, origen, destino, correlation ID, idempotency key | `shipmentId`, guía, estado | Datos inválidos, timeout, indisponible |

## Reglas transversales propuestas

- Ningún resultado exitoso se infiere a partir de un timeout.
- Cada operación reintentable define idempotencia antes de integrarse.
- Todos los mensajes usan el sobre de eventos v1 cuando la interacción sea asíncrona.
- La Saga persiste su progreso antes de continuar al siguiente paso.
- Orders no escribe tablas pertenecientes a los módulos participantes.
- Las compensaciones se documentan y aprueban antes de implementar ORD-04.

## Aprobaciones pendientes

| Puerto | Propietario | Estado | Evidencia |
|---|---|---|---|
| Inventory | Jorge Pumay | Pendiente | Pendiente |
| Payments | Edson Sosa | Pendiente | Pendiente |
| Fiscal | Andy Sánchez | Pendiente | Pendiente |
| Logistics | Kevin Girón | Pendiente | Pendiente |

## Estado de materialización local

- `FiscalService` implementa la solicitud idempotente, el documento `FISCAL_PENDING`, la autorización/rechazo SAT y la consulta de la compuerta de confirmación. Trabaja contra esta propuesta y ADR-010; no convierte el contrato en aceptado.
- `PaymentAttemptService` materializa el puerto de Payments (PAY-01) en `payments.api.PaymentPort`: recibe orden, importe, referencia segura, correlación e idempotencia y devuelve intento, estado e instante; persiste `payment_attempts` y deja evidencia auditable. El timeout y la conciliación pertenecen a PAY-02/PAY-03.
- `ShipmentService` materializa el puerto de Logistics según la entrega de Kevin.
- ORD-03 sigue pendiente: Jairo debe consumir APIs públicas/eventos y no las entidades o repositorios internos.
- El despacho durable mediante Outbox/Inbox ya cuenta con BB-04; su conexión a cada módulo sigue pendiente.
