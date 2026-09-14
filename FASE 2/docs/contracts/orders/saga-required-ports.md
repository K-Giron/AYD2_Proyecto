# ORD-03 — Puertos requeridos por la Saga

**Estado:** `PROPOSED`

**Implementación:** no iniciada

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
