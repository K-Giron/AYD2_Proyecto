-- Directorio de clientes en el servidor: un customerId fijo por usuario,
-- para que el carrito (que orders conserva por customerId) sea el mismo
-- desde la web, otro dispositivo o la tienda física (CDU-01).
CREATE TABLE customers (
    id                  UUID PRIMARY KEY,
    username            VARCHAR(160) NOT NULL,
    full_name           VARCHAR(160) NOT NULL,
    registered_channel  VARCHAR(24) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_customers_username UNIQUE (username),
    CONSTRAINT ck_customers_channel CHECK (registered_channel IN ('WEB', 'MOBILE', 'STORE', 'CALL_CENTER'))
);

-- Cliente de demostración con el mismo id fijo que ya usa la web
-- (session.js), para no dejar huérfanos los carritos ni las órdenes existentes.
INSERT INTO customers (id, username, full_name, registered_channel, created_at) VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'cliente@megastore.com', 'Cliente de demostración', 'WEB', now());
