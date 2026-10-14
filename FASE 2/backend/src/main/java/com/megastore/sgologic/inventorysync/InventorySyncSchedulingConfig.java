package com.megastore.sgologic.inventorysync;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Planificador propio de inventorysync para los reintentos hacia
 * inventory-service, mismo patrón que {@code LogisticsSchedulingConfig}
 * (LOG-03): no se reutiliza infraestructura de otro módulo.
 */
@Configuration
class InventorySyncSchedulingConfig {

    @Bean
    TaskScheduler inventorySyncRetryScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("inventorysync-retry-");
        scheduler.initialize();
        return scheduler;
    }
}
