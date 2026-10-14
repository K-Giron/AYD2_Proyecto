-- La venta confirmada debe conservar su ruta financiera hasta el ERP.
-- Los registros de Fase 2 eran pagos PayKlik; STORE representa cobro en POS.
ALTER TABLE erp_sales ADD COLUMN financial_route VARCHAR(24);

UPDATE erp_sales
SET financial_route = CASE WHEN channel = 'STORE' THEN 'STORE' ELSE 'PAYKLIK' END
WHERE financial_route IS NULL;

ALTER TABLE erp_sales ALTER COLUMN financial_route SET NOT NULL;
ALTER TABLE erp_sales ADD CONSTRAINT ck_erp_sales_financial_route
    CHECK (financial_route IN ('PAYKLIK', 'OWN_CREDIT', 'STORE'));

CREATE INDEX ix_erp_sales_financial_route_confirmed
    ON erp_sales (financial_route, confirmed_at);
