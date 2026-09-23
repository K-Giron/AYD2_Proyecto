# Migraciones

Directorio reservado para migraciones Flyway.

`V1__inventory_create_tables.sql` (BB-03, Jorge Pumay) crea el esquema lógico
propio del módulo `inventory` (`inventory_locations`, `inventory_items`,
`inventory_reservations`), conforme a ADR-011 y CDU-02. No crea tablas de
`orders`: la migración inicial de `orders` queda pendiente para cuando Jairo
inicie ORD-01/ORD-02, usando el siguiente número disponible dentro del rango
reservado abajo.

`V2__inventory_seed_locations_and_stock.sql` (INV-01, Jorge Pumay) carga las
34 ubicaciones (2 CD + 32 tiendas) y stock de demostración para tres
productos de identificador fijo, documentados en el propio archivo para que
Carrito/Órdenes los reutilice en sus datos de prueba.

`V3001__integration_create_outbox_tables.sql` (BB-04, Edson Sosa) crea
`outbox_event` e `inbox_event` en el esquema lógico de `integration`,
conforme a ADR-002 y al sobre de eventos `docs/contracts/events/event-envelope-v1.md`.

## Rangos ratificados (2026-09-23)

El equipo ratificó la convención de rangos para evitar colisiones entre
propietarios. Cada nueva migración usa el siguiente número libre dentro del
rango de su módulo:

| Rango | Módulo | Propietario |
|---|---|---|
| V1-V99 | orders / inventory | Jorge Pumay (inventory) / Jairo Gómez (orders) |
| V1001-V1099 | logistics, notifications, query, incidents | Kevin Girón |
| V2001-V2099 | fiscal, ERP y POS | Andy Sánchez |
| V3001-V3999 | integration (Outbox/Inbox), payments, audit | Edson Sosa |

Estado de uso: `V1`, `V2`; `V1001`-`V1004`; `V2001`-`V2003`; `V3001`. Las
migraciones de `payments` y `audit` (PAY-01 y AUD-01) continúan libres dentro
de `V3001`-`V3999`.
