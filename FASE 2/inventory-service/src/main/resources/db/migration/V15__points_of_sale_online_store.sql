-- Punto de venta "Tienda Online": representa la web del monolito
-- (FASE 2/frontend + backend, puerto 8081/8080). A diferencia de las
-- tiendas físicas, su stock se alimenta por una integración en vivo
-- (ver módulo inventorysync del monolito), no por folios manuales.
INSERT INTO warehouses (id, code, name) VALUES
    ('a1000000-0000-0000-0000-000000000005', 'ALM-POS-ONLINE', 'Almacén Tienda Online');

INSERT INTO points_of_sale (id, code, name, warehouse_id) VALUES
    ('a2000000-0000-0000-0000-000000000004', 'POS-ONLINE', 'Tienda Online',
     'a1000000-0000-0000-0000-000000000005');
