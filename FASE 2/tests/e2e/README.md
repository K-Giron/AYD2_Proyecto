# Suite E2E de órdenes

**Estado:** implementada en `OrderJourneysE2ETest` con PostgreSQL y RabbitMQ de Testcontainers.

**Tarjetas relacionadas:** ORD-05 y QA-04.

## Ejecución

```bash
cd "FASE 2/backend"
./mvnw -Dtest=OrderJourneysE2ETest test
```

Los datos conocidos están en `tests/data/ord-05-deterministic-fixtures.json`.

## Escenarios automatizados

1. Login → carrito → compra → SAT → despacho.
2. Recuperación del carrito en otro canal → compra → repetición idempotente de Saga.
3. Login → carrito → reserva → cancelación → liberación idempotente.
4. Login → compra → despacho → tránsito → entrega.
5. Login → compra → entrega → devolución → reembolso administrativo.

## Camino feliz cubierto

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

## Reglas de la suite

- Se ejecutará desde ambiente limpio con un solo comando.
- Usará IDs deterministas o capturados durante la ejecución, nunca IDs productivos.
- Validará estados y efectos observables, no tablas privadas.
- Conservará un correlation ID común.
- Limpiará únicamente datos creados por la prueba mediante mecanismos acordados.
- Verifica estados y efectos observables mediante HTTP, sin leer tablas privadas.
