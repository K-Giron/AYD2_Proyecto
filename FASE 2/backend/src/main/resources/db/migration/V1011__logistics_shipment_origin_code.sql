-- Módulo: logistics (Kevin Girón)
-- El despacho guarda además del tipo de origen (centro de distribución,
-- tienda, recogida) el almacén concreto del que sale: ALM-CENTRAL o el
-- almacén de una tienda en el almacén central, o CD-WEB para la tienda
-- online. Nulo en despachos anteriores a este cambio.
ALTER TABLE shipments ADD COLUMN origin_code VARCHAR(32);
