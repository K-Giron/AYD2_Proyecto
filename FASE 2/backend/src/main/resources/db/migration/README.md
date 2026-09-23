# Migraciones

Directorio reservado para migraciones Flyway.

`V1__inventory_create_tables.sql` (BB-03, Jorge Pumay) crea el esquema lógico
propio del módulo `inventory` (`inventory_locations`, `inventory_items`,
`inventory_reservations`), conforme a ADR-011 y CDU-02. No crea tablas de
`orders`: la migración inicial de `orders` queda pendiente para cuando Jairo
inicie ORD-01/ORD-02, usando el siguiente número disponible dentro del rango
reservado abajo. Los datos semilla de las 34 ubicaciones (INV-01) se
agregarán en una migración `V2` posterior, separada de la creación de tablas.

## Numeración propuesta por Kevin Girón (pendiente de ratificar con el equipo)

Como BB-03 aún no reservaba un rango de versiones cuando se escribió esta nota, las migraciones de Logistics, Notifications, Query e Incidents usan el rango `V1001`-`V1099` para reducir el riesgo de colisión con las migraciones que Jorge (inventory), Edson (BB-04/Outbox) y Andy (Fiscal/ERP/POS) todavía deben agregar:

| Rango | Módulo | Propietario |
|---|---|---|
| V1-V99 (sugerido) | orders / inventory | Jorge Pumay (inventory) / Jairo Gómez (orders) |
| V1001-V1099 | logistics, notifications, query, incidents | Kevin Girón |

Esta convención no está aprobada por el arquitecto integrador; si el equipo acuerda otra numeración en el daily, estas migraciones deben renumerarse antes de integrar a `develop`.
