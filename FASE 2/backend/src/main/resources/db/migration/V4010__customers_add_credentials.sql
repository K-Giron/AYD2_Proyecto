-- Credenciales propias para clientes registrados en la web.
-- Las cuentas internas continúan externalizadas en sgo.security.users; por eso
-- el hash es opcional para el cliente canónico ya existente y para registros
-- creados desde canales asistidos que todavía no poseen acceso web.
ALTER TABLE customers
    ADD COLUMN password_hash VARCHAR(100);

