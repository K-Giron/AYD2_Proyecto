-- Módulo: incidents (Kevin Girón)
-- customer_id opcional: lo entrega quien registra la incidencia. Permite el
-- historial de atenciones del cliente a través de todas sus órdenes sin
-- que Incidents lea tablas de Orders para resolverlo.

ALTER TABLE incidents ADD COLUMN customer_id UUID;

CREATE INDEX ix_incidents_customer_id ON incidents (customer_id);
