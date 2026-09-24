# Migraciones

Directorio reservado para migraciones Flyway.

`V1__inventory_create_tables.sql` (BB-03, Jorge Pumay) crea el esquema lógico
propio del módulo `inventory` (`inventory_locations`, `inventory_items`,
`inventory_reservations`), conforme a ADR-011 y CDU-02.

`V3001__orders_create_cart_and_order_tables.sql` materializa ORD-01 y la base de
ORD-02: carrito omnicanal, artículos, orden, líneas e historial de estados.
Los módulos mantienen propiedad separada y se correlacionan por UUID.

`V3002__payments_create_attempts.sql` materializa PAY-01: intentos idempotentes,
resultado auditable y referencia tokenizada sin PAN ni CVV. El rango V3001+
permite aplicar estas entregas sobre instalaciones que ya alcanzaron V2003.

`V4001__messaging_and_audit_create_tables.sql` materializa la base de ADR-002 y
CUN-12: Outbox transaccional con reintento/DLQ, evidencia append-only y registro
de búsquedas y exportaciones. Las transiciones de orden ya escriben historial,
Outbox y auditoría en la misma transacción; falta conectar los consumidores de
ERP y POS para cerrar BB-04 de extremo a extremo. Notifications ya consume la
confirmación de orden mediante Inbox.

`V4002__messaging_create_inbox.sql` agrega el libro Inbox idempotente exigido
por ADR-002. El primer consumidor RabbitMQ usa esta tabla para proyectar los
eventos `ORDER_STATUS_CHANGED` en la consulta consolidada de CUN-09 sin aplicar
dos veces un mensaje duplicado.

`V4003__notifications_create_customer_contacts.sql` materializa contactos
confirmados y preferencia de canal para CUN-11/ADR-008. Incluye únicamente el
contacto de la cuenta CLIENTE de la demostración E2E; el alta productiva de
contactos continúa pendiente de seguridad y gestión de perfil.

`V2__inventory_seed_locations_and_stock.sql` (INV-01, Jorge Pumay) carga las
34 ubicaciones (2 CD + 32 tiendas) y stock de demostración para tres
productos de identificador fijo, documentados en el propio archivo para que
Carrito/Órdenes los reutilice en sus datos de prueba.

## Numeración propuesta por Kevin Girón (pendiente de ratificar con el equipo)

Como BB-03 aún no reservaba un rango de versiones cuando se escribió esta nota, las migraciones de Logistics, Notifications, Query e Incidents usan el rango `V1001`-`V1099` para reducir el riesgo de colisión con las migraciones que Jorge (inventory), Edson (BB-04/Outbox) y Andy (Fiscal/ERP/POS) todavía deben agregar:

| Rango | Módulo | Propietario |
|---|---|---|
| V1-V99 | inventory | Jorge Pumay |
| V1001-V1099 | logistics, notifications, query, incidents | Kevin Girón |
| V2001-V2099 | fiscal, ERP y POS | Andy Sánchez |
| V3001-V3099 | orders y payments | Jairo Gómez / integración MVP |
| V4001-V4099 | messaging y audit transversal | Integración MVP / revisión de Edson |

Esta convención no está aprobada por el arquitecto integrador; si el equipo acuerda otra numeración en el daily, estas migraciones deben renumerarse antes de integrar a `develop`.
