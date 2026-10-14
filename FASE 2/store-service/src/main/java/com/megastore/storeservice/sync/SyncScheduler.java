package com.megastore.storeservice.sync;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Sincronización automática; se apaga con store.sync.enabled=false (pruebas). */
@Component
@EnableScheduling
@ConditionalOnProperty(prefix = "store.sync", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SyncScheduler {

    private final SyncService syncService;

    public SyncScheduler(SyncService syncService) {
        this.syncService = syncService;
    }

    @Scheduled(initialDelayString = "PT5S", fixedDelayString = "${store.sync.pull-interval:PT15S}")
    void pullReceipts() {
        syncService.pullReceipts(false);
    }

    @Scheduled(initialDelayString = "PT10S", fixedDelayString = "${store.sync.push-interval:PT30S}")
    void pushPending() {
        syncService.pushPending();
    }
}
