package com.megastore.sgologic.inventory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Planificador propio de Inventory para el barrido periódico de INV-03. No
 * se reutiliza infraestructura de otro módulo (mismo criterio que
 * {@code logistics.LogisticsSchedulingConfig}) ni se modifica configuración
 * compartida.
 */
@Configuration
class InventoryExpirationConfig {

    @Bean
    TaskScheduler inventoryExpirationScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("inventory-expiration-");
        scheduler.initialize();
        return scheduler;
    }
}
