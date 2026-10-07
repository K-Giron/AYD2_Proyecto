package com.megastore.inventoryservice.posevents;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "inventory.pos-events.enabled", havingValue = "true", matchIfMissing = true)
class PosStockEventScheduler {

    private final PosStockEventService service;

    PosStockEventScheduler(PosStockEventService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${inventory.pos-events.interval:PT3S}", initialDelayString = "PT10S")
    void deliver() {
        service.deliverDue();
    }
}
