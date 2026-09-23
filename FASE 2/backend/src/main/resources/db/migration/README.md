# Migraciones

Directorio reservado para migraciones Flyway. La migración relacional inicial (BB-03, tablas de Orders/Inventory) corresponde a Jorge Pumay y todavía no existe en este incremento.

## Numeración propuesta por Kevin Girón (pendiente de ratificar con el equipo)

Como BB-03 aún no reserva un rango de versiones y Flyway solo exige enteros ascendientes únicos, las migraciones de Logistics, Notifications, Query e Incidents usan el rango `V1001`-`V1099` para reducir el riesgo de colisión con las migraciones que Jorge (BB-03), Edson (BB-04/Outbox) y Andy (Fiscal/ERP/POS) todavía deben agregar:

| Rango | Módulo | Propietario |
|---|---|---|
| V1-V99 (sugerido) | orders / inventory | Jorge Pumay |
| V1001-V1099 | logistics, notifications, query, incidents | Kevin Girón |
| V2001-V2099 | fiscal, ERP y POS | Andy Sánchez |

Esta convención no está aprobada por el arquitecto integrador; si el equipo acuerda otra numeración en el daily, estas migraciones deben renumerarse antes de integrar a `develop`.
