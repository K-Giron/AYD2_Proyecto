package com.megastore.sgologic.fiscal;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.Executor;

@Configuration
class FiscalExecutionConfig {

    @Bean
    Executor fiscalSubmissionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(250);
        executor.setThreadNamePrefix("fiscal-submission-");
        executor.initialize();
        return executor;
    }

    @Bean
    TaskScheduler fiscalRetryScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("fiscal-retry-");
        scheduler.initialize();
        return scheduler;
    }
}
