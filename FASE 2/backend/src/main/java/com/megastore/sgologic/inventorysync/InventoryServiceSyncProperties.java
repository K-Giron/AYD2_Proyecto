package com.megastore.sgologic.inventorysync;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Credenciales y conexión hacia inventory-service. Cuenta de servicio
 * propia (rol LOGISTICA), no una cuenta humana — ver {@code .env.example}.
 */
@Component
@ConfigurationProperties(prefix = "sgo.integrations.inventory-service")
public class InventoryServiceSyncProperties {

    private String baseUrl = "http://inventory-service:8080";
    private String username = "integracion-web@almacencentral.local";
    private String password = "IntegracionWeb123";
    private long timeoutMs = 5000L;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public long getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; }
}
