-- Módulo: inventory (Jorge Pumay)
-- INV-01: datos semilla de las 34 ubicaciones (2 centros de distribución +
-- 32 tiendas, DDA §2.2) y stock inicial de productos de demostración,
-- conforme a ADR-011. Estos NO son datos de inventario real de MegaStore:
-- son semilla simulada para demostrar el camino feliz y la concurrencia de
-- INV-02/INV-05 en un ambiente reproducible.
--
-- SGO-Logic no gestiona catálogo de productos (DDA §2.2: fuera de alcance).
-- Los product_id de abajo son identificadores fijos de demostración,
-- documentados aquí para que Carrito/Órdenes (Jairo) los referencie en sus
-- propios datos de prueba sin necesidad de un módulo de catálogo:
--   11111111-1111-1111-1111-111111111101  Camisa manga larga azul
--   11111111-1111-1111-1111-111111111102  Pantalón de lona negro
--   11111111-1111-1111-1111-111111111103  Zapatos deportivos blancos

INSERT INTO inventory_locations (id, code, name, type, created_at) VALUES
    (gen_random_uuid(), 'CD-01', 'Centro de Distribución 1 - Ciudad de Guatemala', 'DISTRIBUTION_CENTER', now()),
    (gen_random_uuid(), 'CD-02', 'Centro de Distribución 2 - San Salvador', 'DISTRIBUTION_CENTER', now());

INSERT INTO inventory_locations (id, code, name, type, created_at)
SELECT gen_random_uuid(),
       'ST-' || lpad(n::text, 2, '0'),
       'Tienda ' || lpad(n::text, 2, '0'),
       'STORE',
       now()
FROM generate_series(1, 32) AS n;

-- Stock amplio en ambos centros de distribución para el camino feliz.
INSERT INTO inventory_items (id, product_id, location_id, quantity_on_hand, quantity_reserved, updated_at)
SELECT gen_random_uuid(), '11111111-1111-1111-1111-111111111101', id, 5, 0, now()
FROM inventory_locations WHERE code = 'CD-01';

INSERT INTO inventory_items (id, product_id, location_id, quantity_on_hand, quantity_reserved, updated_at)
SELECT gen_random_uuid(), '11111111-1111-1111-1111-111111111102', id, 20, 0, now()
FROM inventory_locations WHERE code = 'CD-01';

INSERT INTO inventory_items (id, product_id, location_id, quantity_on_hand, quantity_reserved, updated_at)
SELECT gen_random_uuid(), '11111111-1111-1111-1111-111111111103', id, 8, 0, now()
FROM inventory_locations WHERE code = 'CD-02';

-- Una sola unidad en una tienda: permite demostrar INV-02/INV-05 (dos
-- solicitudes concurrentes por la última unidad producen exactamente una
-- reserva confirmada, conforme a CDU-02 E1).
INSERT INTO inventory_items (id, product_id, location_id, quantity_on_hand, quantity_reserved, updated_at)
SELECT gen_random_uuid(), '11111111-1111-1111-1111-111111111101', id, 1, 0, now()
FROM inventory_locations WHERE code = 'ST-01';
