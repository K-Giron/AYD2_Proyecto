-- Reemplaza el destinatario de texto libre por una referencia real al
-- punto de venta (identificador + almacén asignado).
ALTER TABLE stock_movements ADD COLUMN point_of_sale_id UUID REFERENCES points_of_sale (id);
ALTER TABLE stock_movements DROP COLUMN destinatario;
