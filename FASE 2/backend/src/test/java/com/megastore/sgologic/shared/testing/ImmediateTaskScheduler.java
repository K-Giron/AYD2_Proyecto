package com.megastore.sgologic.shared.testing;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

/** Planificador sin esperas reales para probar politicas de reintento. */
public final class ImmediateTaskScheduler implements TaskScheduler {
    @Override
    public ScheduledFuture<?> schedule(Runnable task, Instant startTime) {
        task.run();
        return null;
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
        task.run();
        return null;
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Instant startTime, Duration period) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Duration period) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Instant startTime, Duration delay) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Duration delay) {
        throw new UnsupportedOperationException();
    }
}
