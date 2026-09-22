package com.megastore.sgologic.logistics;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Planificador propio de Logistics para los reintentos de guía de
 * LogiExpress (LOG-03). No se reutiliza infraestructura de otro módulo ni
 * se modifica configuración compartida. El reloj es el único bean
 * {@code Clock} de la aplicación, declarado en {@code shared}
 * ({@code SharedClockConfig}).
 */
@Configuration
class LogisticsSchedulingConfig {

    @Bean
    TaskScheduler logisticsRetryScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("logistics-retry-");
        scheduler.initialize();
        return scheduler;
    }
}
