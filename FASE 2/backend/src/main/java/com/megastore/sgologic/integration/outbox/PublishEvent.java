package com.megastore.sgologic.integration.outbox;

import java.util.UUID;

/**
 * API pública del Outbox para módulos propietarios (BB-04, ADR-002).
 *
 * <p>La implementación exige una transacción activa
 * ({@code Propagation.MANDATORY}): el cambio de negocio y el registro del
 * evento deben confirmarse o revertirse juntos. Un consumidor nunca debe
 * publicar directamente en RabbitMQ.</p>
 */
public interface PublishEvent {

    /**
     * Registra el evento en el Outbox y devuelve su identificador.
     *
     * @throws org.springframework.transaction.IllegalTransactionStateException
     *         si no existe una transacción de negocio activa
     */
    UUID publish(OutboxEventRequest request);
}
