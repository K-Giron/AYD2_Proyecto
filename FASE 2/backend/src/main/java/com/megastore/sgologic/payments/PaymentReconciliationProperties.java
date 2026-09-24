package com.megastore.sgologic.payments;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Configuración externalizada de la conciliación (PAY-03, ADR-003): el
 * primer trabajo se programa dentro de los 60 s, los reintentos usan
 * backoff y se alerta si el caso sigue sin resolver tras
 * {@code alert-after}.
 */
@Component
@ConfigurationProperties(prefix = "sgo.payments.reconciliation")
public class PaymentReconciliationProperties {

    private boolean enabled = true;
    private long pollIntervalMs = 5000L;
    private int batchSize = 20;
    private int maxAttempts = 4;
    private Duration firstDelay = Duration.ofSeconds(30);
    private List<Duration> retryDelays =
            List.of(Duration.ofSeconds(60), Duration.ofSeconds(120), Duration.ofSeconds(240));
    private Duration alertAfter = Duration.ofMinutes(5);

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public long getPollIntervalMs() { return pollIntervalMs; }
    public void setPollIntervalMs(long pollIntervalMs) { this.pollIntervalMs = pollIntervalMs; }
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    public Duration getFirstDelay() { return firstDelay; }
    public void setFirstDelay(Duration firstDelay) { this.firstDelay = firstDelay; }
    public List<Duration> getRetryDelays() { return retryDelays; }
    public void setRetryDelays(List<Duration> retryDelays) { this.retryDelays = retryDelays; }
    public Duration getAlertAfter() { return alertAfter; }
    public void setAlertAfter(Duration alertAfter) { this.alertAfter = alertAfter; }
}
