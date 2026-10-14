package com.megastore.inventoryservice.posevents;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Sistema conectado de cada punto de venta: dónde se le entregan los eventos
 * de stock y, si lo exige, con qué cuenta se autentica el central.
 * Un punto de venta sin terminal registrado acumula sus eventos como
 * SIN_TERMINAL hasta que se conecte.
 */
@Component
@ConfigurationProperties(prefix = "inventory.pos-events")
public class PosTerminalProperties {

    private boolean enabled = true;
    private Duration interval = Duration.ofSeconds(3);
    private int timeoutMs = 5000;
    private Map<String, Terminal> terminals = new HashMap<>();

    public static class Terminal {
        private String url;
        private String loginUrl;
        private String username;
        private String password;

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getLoginUrl() { return loginUrl; }
        public void setLoginUrl(String loginUrl) { this.loginUrl = loginUrl; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        boolean connected() { return url != null && !url.isBlank(); }
        boolean requiresLogin() { return loginUrl != null && !loginUrl.isBlank(); }
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Duration getInterval() { return interval; }
    public void setInterval(Duration interval) { this.interval = interval; }
    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    public Map<String, Terminal> getTerminals() { return terminals; }
    public void setTerminals(Map<String, Terminal> terminals) { this.terminals = terminals; }
}
