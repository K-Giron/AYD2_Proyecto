package com.megastore.sgologic.integration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.Executor;

@Configuration
@EnableScheduling
class IntegrationExecutionConfig implements SchedulingConfigurer {

    @Bean
    Executor integrationDispatchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("integration-dispatch-");
        executor.initialize();
        return executor;
    }

    @Bean
    TaskScheduler erpRetryScheduler() {
        return scheduler("erp-retry-", 1);
    }

    @Bean
    TaskScheduler posRetryScheduler() {
        return scheduler("pos-retry-", 2);
    }

    @Bean
    TaskScheduler integrationTaskScheduler() {
        return scheduler("integration-window-", 1);
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setTaskScheduler(integrationTaskScheduler());
    }

    private TaskScheduler scheduler(String prefix, int poolSize) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(poolSize);
        scheduler.setThreadNamePrefix(prefix);
        scheduler.initialize();
        return scheduler;
    }
}
