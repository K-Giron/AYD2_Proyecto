-- Búsqueda manual en la caja (código, nombre o descripción) y reimpresión
-- de facturas. La descripción viene del artículo del central con cada envío.

ALTER TABLE store_articles ADD COLUMN description VARCHAR(500);
ALTER TABLE store_receipts ADD COLUMN description VARCHAR(500);

-- Cada reimpresión queda contada: la copia impresa dice "REIMPRESIÓN n".
ALTER TABLE sales
    ADD COLUMN reprint_count     INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN last_reprinted_at TIMESTAMPTZ;

CREATE INDEX idx_sales_created_at ON sales (created_at DESC);
