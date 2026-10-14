-- Tienda física donde se cobró cada orden (registrada por la terminal de
-- tienda). Sin fila aquí, la orden es una venta web. order_id es solo un
-- identificador por valor: este módulo no es dueño de la tabla de órdenes.
CREATE TABLE inventorysync_order_origins (
    order_id            UUID PRIMARY KEY,
    point_of_sale_code  VARCHAR(32) NOT NULL,
    registered_at       TIMESTAMPTZ NOT NULL
);
