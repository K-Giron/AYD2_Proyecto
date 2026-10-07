package com.megastore.storeservice.sync;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Component
public class SyncLog {

    public static final String SUBIDA = "SUBIDA";
    public static final String BAJADA = "BAJADA";

    private final SyncLogRepository repository;
    private final Clock clock;

    public SyncLog(SyncLogRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** Transacción propia: el rastro queda aunque falle lo que se estaba sincronizando. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String direction, String kind, String reference, boolean ok, String detail) {
        repository.save(SyncLogEntry.of(direction, kind, reference, ok, detail, clock.instant()));
    }

    @Transactional(readOnly = true)
    public List<SyncLogEntry> recent() {
        return repository.findTop300ByOrderByOccurredAtDesc();
    }
}
