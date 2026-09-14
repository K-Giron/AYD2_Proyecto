# Plan preparatorio de E2E

**Estado:** `PROPOSED`; no existe todavía una prueba E2E funcional.

**Tarjetas relacionadas:** ORD-05 y QA-04.

## Prerrequisitos para automatizar

- Contratos de estados, eventos y errores aceptados.
- BB-03 y migraciones reproducibles.
- Capacidades P0 de Inventory, Payments, Fiscal y Logistics.
- Datos de demostración versionados y no sensibles.
- Estrategia de Outbox/idempotencia disponible cuando el flujo la necesite.

## Camino feliz objetivo

1. Crear o recuperar un carrito desde un canal.
2. Recuperar el mismo carrito desde un segundo canal.
3. Crear la intención de orden.
4. Reservar inventario y asignar origen.
5. Aprobar el pago.
6. Generar y confirmar la información fiscal.
7. Confirmar y preparar la orden.
8. Crear guía, despachar y actualizar seguimiento.
9. Registrar entrega y solicitud de notificación.
10. Consultar la vista consolidada y evidencia correlacionada.

## Reglas de la futura suite

- Se ejecutará desde ambiente limpio con un solo comando.
- Usará IDs deterministas o capturados durante la ejecución, nunca IDs productivos.
- Validará estados y efectos observables, no tablas privadas.
- Conservará un correlation ID común.
- Limpiará únicamente datos creados por la prueba mediante mecanismos acordados.
- No se marcará como implementada hasta integrar todas las líneas requeridas.
