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
| V4001-V4099 | orders, Inbox de consumidores y contactos | Jairo Gómez / integración MVP |

Estado de uso: `V1`-`V3`; `V1001`-`V1006`; `V2001`-`V2003`;
`V3001`-`V3004`; y `V4001`-`V4003`.

`V1005__notifications_create_dead_letters_and_order_links.sql` (Kevin Girón)
agrega la DLQ del trabajador de notificaciones (`notification_dead_letters`,
simétrica a `shipment_dead_letters` de LOG-03) y `order_customer_links`, la
resolución local `orderId -> customerId` que permite a Notifications avisar
sobre eventos de despacho (Logistics) e incidencia (Incidents) sin que esos
módulos conozcan al cliente ni lean tablas de Orders.

`V1006__incidents_add_customer_id.sql` (Kevin Girón) agrega `customer_id`
opcional a `incidents`, para poder consultar el historial de un cliente a
través de todas sus órdenes, no solo de una orden puntual.

`V3__inventory_create_stock_movements.sql` (Kevin Girón, a solicitud directa
sobre el módulo de Jorge Pumay) crea `inventory_stock_movements`: entradas y
salidas de inventario fuera del flujo de reserva de venta en línea
(recepción de mercadería, merma, ajuste de tienda/POS), con el mismo patrón
de escritura atómica de `InventoryItemRepository#tryReserve` y publicación
al Outbox en la misma transacción.
