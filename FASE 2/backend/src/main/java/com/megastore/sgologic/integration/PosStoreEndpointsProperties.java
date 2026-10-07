package com.megastore.sgologic.integration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Tiendas cuyo POS local recibe los eventos por HTTP real (número de tienda →
 * URL del endpoint de eventos de su sistema). Las que no estén aquí siguen en
 * el simulador BB-06. Hoy: la tienda 1 es POS-01 (store-service).
 */
@Component
@ConfigurationProperties(prefix = "sgo.integrations.pos")
public class PosStoreEndpointsProperties {

    private Map<Integer, String> storeEndpoints = new HashMap<>();
    private long timeoutMs = 5000L;

    public Map<Integer, String> getStoreEndpoints() { return storeEndpoints; }
    public void setStoreEndpoints(Map<Integer, String> storeEndpoints) { this.storeEndpoints = storeEndpoints; }
    public long getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; }
}
