-- Datos de demostración para poder probar entradas y salidas de inmediato.
INSERT INTO articles (id, sku, name, description, price, created_at) VALUES
    ('b1000000-0000-0000-0000-000000000001', 'MS-CAM-101', 'Camisa manga larga azul', 'Camisa de corte clásico en azul', 249.00, now()),
    ('b1000000-0000-0000-0000-000000000002', 'MS-PAN-102', 'Pantalón de lona negro', 'Pantalón negro de uso diario', 329.00, now()),
    ('b1000000-0000-0000-0000-000000000003', 'MS-ZAP-103', 'Zapatos deportivos blancos', 'Zapatos deportivos blancos', 599.00, now());

INSERT INTO stock (article_id, quantity_on_hand, updated_at, version) VALUES
    ('b1000000-0000-0000-0000-000000000001', 40, now(), 0),
    ('b1000000-0000-0000-0000-000000000002', 25, now(), 0),
    ('b1000000-0000-0000-0000-000000000003', 15, now(), 0);
