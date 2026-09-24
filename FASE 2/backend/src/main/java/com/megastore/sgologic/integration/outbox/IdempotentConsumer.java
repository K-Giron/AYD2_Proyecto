package com.megastore.sgologic.integration.outbox;

import java.util.UUID;

/**
 * API pública para consumidores de eventos (BB-04).
 *
 * <p>Un consumidor debe llamar a {@link #registerProcessed(String, UUID)}
 * antes de aplicar el efecto de negocio. Si devuelve {@code false}, el
 * evento ya fue procesado por esa cola y debe descartarse sin repetir el
 * efecto (semántica al menos una vez, consumidor idempotente).</p>
 */
public interface IdempotentConsumer {

    /**
     * Registra el evento como procesado para el consumidor indicado.
     *
     * @return {@code true} si es la primera vez; {@code false} si ya estaba
     *         registrado y el evento debe descartarse
     */
    boolean registerProcessed(String consumer, UUID eventId);
}
