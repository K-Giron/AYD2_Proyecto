-- La mercadería que envía el central ya no entra sola a la existencia: se
-- baja como EN_TRANSITO y solo suma cuando alguien de la tienda escribe el
-- folio del central (SAL-… o TRA-…), revisa los artículos y confirma.
-- La recepción guarda los datos del artículo porque puede ser un artículo
-- que la tienda todavía no conoce (se da de alta al confirmar).

ALTER TABLE store_receipts DROP CONSTRAINT store_receipts_article_id_fkey;

ALTER TABLE store_receipts
    ADD COLUMN origin       VARCHAR(32),
    ADD COLUMN sku          VARCHAR(64),
    ADD COLUMN name         VARCHAR(160),
    ADD COLUMN price        NUMERIC(12, 2),
    ADD COLUMN status       VARCHAR(16),
    ADD COLUMN received_by  VARCHAR(80);

-- Lo ya aplicado antes de este cambio queda como recibido.
UPDATE store_receipts r
SET origin = CASE WHEN r.origin_folio LIKE 'TRA-%' THEN NULL ELSE 'CENTRAL' END,
    sku = a.sku, name = a.name, price = a.price,
    status = 'RECIBIDO', received_by = 'automático'
FROM store_articles a
WHERE a.id = r.article_id;

ALTER TABLE store_receipts
    ALTER COLUMN sku    SET NOT NULL,
    ALTER COLUMN name   SET NOT NULL,
    ALTER COLUMN price  SET NOT NULL,
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN applied_at DROP NOT NULL,
    ADD CONSTRAINT ck_store_receipts_status CHECK (status IN ('EN_TRANSITO', 'RECIBIDO'));

CREATE INDEX idx_store_receipts_origin_folio ON store_receipts (origin_folio);
CREATE INDEX idx_store_receipts_status ON store_receipts (status);
