package com.megastore.sgologic.integration.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/** Configuración externalizada del Outbox (BB-04). */
@Component
@ConfigurationProperties(prefix = "sgo.outbox")
public class OutboxProperties {

    private boolean enabled = true;
    private long pollIntervalMs = 2000L;
    private int batchSize = 50;
    private int maxAttempts = 4;
    private List<Duration> retryDelays = List.of(Duration.ofSeconds(2), Duration.ofSeconds(8), Duration.ofSeconds(32));
    private String exchange = "sgo.events";
    private String deadLetterExchange = "sgo.events.dlx";
    private String deadLetterQueue = "sgo.events.dlq";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public long getPollIntervalMs() { return pollIntervalMs; }
    public void setPollIntervalMs(long pollIntervalMs) { this.pollIntervalMs = pollIntervalMs; }
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    public List<Duration> getRetryDelays() { return retryDelays; }
    public void setRetryDelays(List<Duration> retryDelays) { this.retryDelays = retryDelays; }
    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }
    public String getDeadLetterExchange() { return deadLetterExchange; }
    public void setDeadLetterExchange(String deadLetterExchange) { this.deadLetterExchange = deadLetterExchange; }
    public String getDeadLetterQueue() { return deadLetterQueue; }
    public void setDeadLetterQueue(String deadLetterQueue) { this.deadLetterQueue = deadLetterQueue; }
}
