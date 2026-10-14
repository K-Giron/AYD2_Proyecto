-- Entradas y umbrales con los que se tomó cada decisión de crédito. Permite
-- reconstruir por qué se aprobó o rechazó aunque el perfil cambie después.
-- Las decisiones anteriores a esta migración quedan sin instantánea (NULL).
ALTER TABLE credit_decisions ADD COLUMN inputs_snapshot TEXT;
