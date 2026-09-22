package com.megastore.sgologic.notifications;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.Executor;

/**
 * Executor y planificador propios de Notifications (EaC-08: la
 * disponibilidad del proveedor no debe bloquear la operación principal). No
 * se reutiliza infraestructura de otro módulo. El reloj es el único bean
 * {@code Clock} de la aplicación, declarado en {@code shared}
 * ({@code SharedClockConfig}).
 */
@Configuration
class NotificationsExecutionConfig {

    @Bean
    Executor notificationsDispatchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("notifications-dispatch-");
        executor.initialize();
        return executor;
    }

    @Bean
    TaskScheduler notificationsRetryScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("notifications-retry-");
        scheduler.initialize();
        return scheduler;
    }
}
