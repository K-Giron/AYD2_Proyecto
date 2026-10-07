package com.megastore.sgologic.onlinestore;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/**
 * Bitácora de sincronización de la tienda online con el almacén central.
 * Pública para que {@code inventorysync} registre aquí las ventas web que
 * reporta al central (descuento de POS-ONLINE).
 */
@Component
public class OnlineSyncLog {

    public static final String SUBIDA = "SUBIDA";
    public static final String BAJADA = "BAJADA";
    public static final String RECEPCION = "RECEPCION";
    public static final String VENTA = "VENTA";
    public static final String TRASLADO = "TRASLADO";

    private final OnlineSyncLogRepository repository;
    private final Clock clock;

    OnlineSyncLog(OnlineSyncLogRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** Transacción propia: el rastro queda aunque falle lo que se estaba sincronizando. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String direction, String kind, String reference, boolean ok, String detail) {
        repository.save(OnlineSyncLogEntry.of(direction, kind, reference, ok, detail, clock.instant()));
    }

    @Transactional(readOnly = true)
    public List<OnlineSyncLogEntry> recent() {
        return repository.findTop300ByOrderByOccurredAtDesc();
    }
}
