package com.megastore.sgologic.audit.api;

import java.util.UUID;

/**
 * API pública del registro auditable transversal (AUD-01, CDU-12).
 *
 * <p>La implementación exige una transacción activa
 * ({@code Propagation.MANDATORY}): la evidencia y el cambio de negocio se
 * confirman o revierten juntos. El actor se toma del contexto autenticado y,
 * si no existe (trabajadores o pruebas), se registra como {@code system}.</p>
 */
public interface AuditRecorder {

    /**
     * Registra la evidencia y devuelve el identificador del evento.
     *
     * @throws org.springframework.transaction.IllegalTransactionStateException
     *         si no existe una transacción de negocio activa
     */
    UUID record(AuditEntryRequest request);
}
