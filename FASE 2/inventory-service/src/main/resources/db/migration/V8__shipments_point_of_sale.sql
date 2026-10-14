-- Todo envío tiene un punto de venta destino conocido.
ALTER TABLE shipments ADD COLUMN point_of_sale_id UUID REFERENCES points_of_sale (id);
UPDATE shipments SET point_of_sale_id = 'a2000000-0000-0000-0000-000000000001' WHERE point_of_sale_id IS NULL;
ALTER TABLE shipments ALTER COLUMN point_of_sale_id SET NOT NULL;
ALTER TABLE shipments DROP COLUMN destinatario;
