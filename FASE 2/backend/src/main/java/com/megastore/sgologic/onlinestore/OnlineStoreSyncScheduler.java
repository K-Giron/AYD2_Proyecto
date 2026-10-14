package com.megastore.sgologic.onlinestore;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Cada 15 s (configurable): baja lo que el central envía a la tienda online y reintenta los traslados pendientes. */
@Component
@ConditionalOnProperty(name = "sgo.integrations.online-store.sync-enabled", havingValue = "true", matchIfMissing = true)
class OnlineStoreSyncScheduler {

    private final OnlineReceivingService receivingService;
    private final OnlineTransferService transferService;

    OnlineStoreSyncScheduler(OnlineReceivingService receivingService, OnlineTransferService transferService) {
        this.receivingService = receivingService;
        this.transferService = transferService;
    }

    @Scheduled(initialDelayString = "${sgo.integrations.online-store.initial-delay-ms:20000}",
            fixedDelayString = "${sgo.integrations.online-store.pull-interval-ms:15000}")
    void pull() {
        try {
            receivingService.pullReceipts();
        } catch (OnlineStoreException offline) {
            // Sin conexión con el central: ya quedó en la bitácora una vez; se reintenta en el próximo ciclo.
            return;
        }
        transferService.pushPending();
    }
}
