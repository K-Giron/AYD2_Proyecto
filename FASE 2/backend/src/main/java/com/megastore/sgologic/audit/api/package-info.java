/**
 * API pública del registro auditable transversal (AUD-01).
 *
 * <p>Los módulos propietarios registran la evidencia de sus operaciones con
 * {@link com.megastore.sgologic.audit.api.AuditRecorder} dentro de la misma
 * transacción del cambio de negocio. El resto del módulo {@code audit}
 * (entidad, repositorio y consultas) es interno.</p>
 */
@org.springframework.modulith.NamedInterface("audit-api")
package com.megastore.sgologic.audit.api;
