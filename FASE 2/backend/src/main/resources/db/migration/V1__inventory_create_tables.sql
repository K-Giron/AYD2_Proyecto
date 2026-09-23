-- Módulo: inventory (Jorge Pumay)
-- Fuente lógica autoritativa de inventario para el MVP (ADR-011, resuelve
-- DEC-03). Tablas propias de INV-01/INV-02/INV-03/INV-04, conforme a CDU-02
-- y EaC-03. No crea ni referencia tablas de otros módulos (orders,
-- logistics, fiscal): la orden y el producto se correlacionan únicamente
-- por order_id/product_id, sin claves foráneas hacia otros esquemas.
--
-- Los datos semilla de las 34 ubicaciones (2 centros de distribución + 32
-- tiendas) se cargan en una migración posterior (INV-01), no en esta.

CREATE TABLE inventory_locations (
    id          UUID PRIMARY KEY,
    code        VARCHAR(16) NOT NULL,
    name        VARCHAR(120) NOT NULL,
    type        VARCHAR(32) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_inventory_locations_code UNIQUE (code),
    CONSTRAINT ck_inventory_locations_type CHECK (type IN ('DISTRIBUTION_CENTER', 'STORE'))
);

-- Stock por producto y ubicación. quantity_reserved <= quantity_on_hand se
-- exige a nivel de base de datos como red de seguridad adicional a la
-- transacción de reserva (INV-02): ninguna combinación de filas puede
-- quedar con disponible negativo, ni siquiera ante un error de aplicación.
CREATE TABLE inventory_items (
    id                 UUID PRIMARY KEY,
    product_id         UUID NOT NULL,
    location_id        UUID NOT NULL REFERENCES inventory_locations (id),
    quantity_on_hand   INTEGER NOT NULL DEFAULT 0,
    quantity_reserved  INTEGER NOT NULL DEFAULT 0,
    updated_at         TIMESTAMPTZ NOT NULL,
    version            BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_inventory_items_product_location UNIQUE (product_id, location_id),
    CONSTRAINT ck_inventory_items_on_hand_non_negative CHECK (quantity_on_hand >= 0),
    CONSTRAINT ck_inventory_items_reserved_non_negative CHECK (quantity_reserved >= 0),
    CONSTRAINT ck_inventory_items_reserved_within_on_hand CHECK (quantity_reserved <= quantity_on_hand)
);

CREATE INDEX ix_inventory_items_product_id ON inventory_items (product_id);
CREATE INDEX ix_inventory_items_location_id ON inventory_items (location_id);

-- Una reserva por intento idempotente (CDU-02). El estado y expires_at
-- permiten la liberación automática de INV-03 sin bloquear la tabla
-- inventory_items completa.
CREATE TABLE inventory_reservations (
    id               UUID PRIMARY KEY,
    order_id         UUID NOT NULL,
    product_id       UUID NOT NULL,
    location_id      UUID NOT NULL REFERENCES inventory_locations (id),
    quantity         INTEGER NOT NULL,
    status           VARCHAR(32) NOT NULL,
    correlation_id   UUID NOT NULL,
    idempotency_key  VARCHAR(128) NOT NULL,
    expires_at       TIMESTAMPTZ NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL,
    updated_at       TIMESTAMPTZ NOT NULL,
    version          BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_inventory_reservations_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT ck_inventory_reservations_quantity_positive CHECK (quantity > 0),
    CONSTRAINT ck_inventory_reservations_status
        CHECK (status IN ('ACTIVE', 'CONFIRMED', 'RELEASED', 'EXPIRED'))
);

CREATE INDEX ix_inventory_reservations_order_id ON inventory_reservations (order_id);

-- Soporta el barrido periódico de INV-03 (reservas ACTIVE vencidas).
CREATE INDEX ix_inventory_reservations_status_expires_at
    ON inventory_reservations (status, expires_at);
