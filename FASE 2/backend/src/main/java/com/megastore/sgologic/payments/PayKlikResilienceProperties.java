package com.megastore.sgologic.payments;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Configuración externalizada de resiliencia de PayKlik (PAY-02, ADR-003):
 * límite de tiempo y circuit breaker. Se aplica solo a esta integración
 * externa, no a llamadas internas del monolito.
 */
@Component
@ConfigurationProperties(prefix = "sgo.payments.payklik")
public class PayKlikResilienceProperties {

    private long timeoutMs = 5000L;
    private int callPoolSize = 8;
    private float failureRateThreshold = 50f;
    private int slidingWindowSize = 10;
    private int minimumNumberOfCalls = 5;
    private Duration waitDurationInOpenState = Duration.ofSeconds(30);
    private int permittedNumberOfCallsInHalfOpenState = 3;

    public long getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; }
    public int getCallPoolSize() { return callPoolSize; }
    public void setCallPoolSize(int callPoolSize) { this.callPoolSize = callPoolSize; }
    public float getFailureRateThreshold() { return failureRateThreshold; }
    public void setFailureRateThreshold(float failureRateThreshold) { this.failureRateThreshold = failureRateThreshold; }
    public int getSlidingWindowSize() { return slidingWindowSize; }
    public void setSlidingWindowSize(int slidingWindowSize) { this.slidingWindowSize = slidingWindowSize; }
    public int getMinimumNumberOfCalls() { return minimumNumberOfCalls; }
    public void setMinimumNumberOfCalls(int minimumNumberOfCalls) { this.minimumNumberOfCalls = minimumNumberOfCalls; }
    public Duration getWaitDurationInOpenState() { return waitDurationInOpenState; }
    public void setWaitDurationInOpenState(Duration waitDurationInOpenState) {
        this.waitDurationInOpenState = waitDurationInOpenState;
    }
    public int getPermittedNumberOfCallsInHalfOpenState() { return permittedNumberOfCallsInHalfOpenState; }
    public void setPermittedNumberOfCallsInHalfOpenState(int permittedNumberOfCallsInHalfOpenState) {
        this.permittedNumberOfCallsInHalfOpenState = permittedNumberOfCallsInHalfOpenState;
    }
}
