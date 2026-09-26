-- Inventario reproducible para el catálogo de demostración de Fase 2.
-- El catálogo comercial permanece en frontend/catalog.json porque SGO-Logic
-- no administra productos (DDA §2.2); esta migración únicamente registra la
-- existencia que necesitan la reserva, la Saga y el despacho.

WITH demo_stock(product_id, location_code, quantity_on_hand) AS (
    VALUES
        ('11111111-1111-1111-1111-111111111101'::uuid, 'CD-01', 25),
        -- ST-01 conserva una única unidad para la demostración concurrente
        -- de INV-05 definida en V2; el stock comercial adicional va a ST-02.
        ('11111111-1111-1111-1111-111111111101'::uuid, 'ST-02', 8),
        ('11111111-1111-1111-1111-111111111102'::uuid, 'CD-01', 30),
        ('11111111-1111-1111-1111-111111111102'::uuid, 'ST-04', 7),
        ('11111111-1111-1111-1111-111111111103'::uuid, 'CD-02', 24),
        ('11111111-1111-1111-1111-111111111103'::uuid, 'ST-08', 6),
        ('11111111-1111-1111-1111-111111111104'::uuid, 'CD-01', 18),
        ('11111111-1111-1111-1111-111111111104'::uuid, 'ST-12', 5),
        ('11111111-1111-1111-1111-111111111105'::uuid, 'CD-02', 16),
        ('11111111-1111-1111-1111-111111111105'::uuid, 'ST-16', 4),
        ('11111111-1111-1111-1111-111111111106'::uuid, 'CD-01', 20),
        ('11111111-1111-1111-1111-111111111106'::uuid, 'ST-20', 5),
        ('11111111-1111-1111-1111-111111111107'::uuid, 'CD-02', 35),
        ('11111111-1111-1111-1111-111111111107'::uuid, 'ST-24', 10),
        ('11111111-1111-1111-1111-111111111108'::uuid, 'CD-01', 15),
        ('11111111-1111-1111-1111-111111111108'::uuid, 'ST-28', 4),
        ('11111111-1111-1111-1111-111111111109'::uuid, 'CD-02', 14),
        ('11111111-1111-1111-1111-111111111109'::uuid, 'ST-30', 3),
        ('11111111-1111-1111-1111-111111111110'::uuid, 'CD-01', 28),
        ('11111111-1111-1111-1111-111111111110'::uuid, 'ST-32', 8)
)
INSERT INTO inventory_items
    (id, product_id, location_id, quantity_on_hand, quantity_reserved, updated_at)
SELECT gen_random_uuid(), stock.product_id, location.id,
       stock.quantity_on_hand, 0, now()
FROM demo_stock stock
JOIN inventory_locations location ON location.code = stock.location_code
ON CONFLICT (product_id, location_id) DO UPDATE
SET quantity_on_hand = GREATEST(inventory_items.quantity_on_hand, EXCLUDED.quantity_on_hand),
    updated_at = now();
