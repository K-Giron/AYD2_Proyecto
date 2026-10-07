-- Ajuste de existencia del almacén central (merma, daño, conteo físico):
-- baja existencia con su propio folio AJU-NNNNNN y motivo obligatorio, sin
-- punto de venta destino. Es la forma correcta de corregir una entrada
-- equivocada: los folios no se borran.
ALTER TABLE stock_movements DROP CONSTRAINT ck_stock_movements_type;
ALTER TABLE stock_movements
    ADD CONSTRAINT ck_stock_movements_type CHECK (movement_type IN ('ENTRADA', 'SALIDA', 'AJUSTE'));

CREATE SEQUENCE seq_folio_ajuste START WITH 1 INCREMENT BY 1;
