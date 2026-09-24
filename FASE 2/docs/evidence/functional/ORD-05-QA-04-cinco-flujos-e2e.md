# Evidencia ORD-03/04/05 y QA-04 — cinco flujos E2E

**Fecha:** 2026-09-23

**Responsable:** Jairo Gómez
**Prueba:** `OrderJourneysE2ETest`

## Alcance

La prueba levanta PostgreSQL 17 y RabbitMQ 4 mediante Testcontainers, habilita JWT y usa los simuladores reproducibles de PayKlik, SAT, LogiExpress y notificaciones. Todas las interacciones de negocio se ejecutan por HTTP con tokens de cliente o STAFF; no se validan resultados leyendo tablas privadas.

## Recorridos

1. Cliente inicia sesión, crea carrito web, compra un producto y la Saga llega a `PREPARING` con despacho.
2. Cliente recupera el mismo carrito desde `MOBILE`, compra y repite la Saga con la misma llave sin duplicar despacho.
3. Cliente crea orden, reserva y cancela antes del pago; la reserva queda `RELEASED` y repetir la cancelación no vuelve a liberar stock.
4. Cliente compra y Logistics conduce despacho y orden por `IN_TRANSIT` hasta `DELIVERED`.
5. Cliente compra, recibe, registra devolución y STAFF resuelve el caso como `REFUNDED` con monto verificable.

## Ejecución reproducible

```bash
cd "FASE 2/backend"
./mvnw -Dtest=OrderJourneysE2ETest test
```

Resultado observado: **5 pruebas, 0 fallos, 0 errores, 0 omitidas**.

Los valores deterministas se documentan en `tests/data/ord-05-deterministic-fixtures.json`. El quinto recorrido verifica el reembolso administrativo disponible en INC-01; la nota de crédito y reposición automática completa continúan perteneciendo al alcance ampliado de CUN-10 y no se presentan como implementadas por ORD-05.
