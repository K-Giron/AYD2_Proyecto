package com.megastore.sgologic.payments;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
class PaymentsExecutionConfig {
    @Bean
    TaskScheduler paymentReconciliationScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("payment-reconciliation-");
        scheduler.initialize();
        return scheduler;
    }
}
