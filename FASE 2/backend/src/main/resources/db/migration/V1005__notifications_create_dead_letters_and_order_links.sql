-- Módulo: notifications (Kevin Girón)
-- DLQ del trabajador de notificaciones (simétrica a shipment_dead_letters,
-- LOG-03) y tabla de resolución local orderId -> customerId, alimentada por
-- ORDER_STATUS_CHANGED, para poder notificar eventos de despacho e
-- incidencia (que no conocen customerId) sin leer tablas de Orders.

CREATE TABLE notification_dead_letters (
    id              UUID PRIMARY KEY,
    notification_id UUID NOT NULL REFERENCES notification_requests (id),
    reason          VARCHAR(300) NOT NULL,
    attempts        INTEGER NOT NULL,
    registered_at   TIMESTAMPTZ NOT NULL
);

CREATE TABLE order_customer_links (
    order_id    UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL
);
