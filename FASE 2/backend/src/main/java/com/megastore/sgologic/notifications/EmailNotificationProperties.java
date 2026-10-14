package com.megastore.sgologic.notifications;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Correo real de notificaciones. {@code provider}: "auto" (por defecto) usa
 * SMTP si hay usuario y contraseña configurados y si no el simulador;
 * "smtp" o "simulated" lo fuerzan.
 */
@Component
@ConfigurationProperties(prefix = "sgo.notifications.email")
public class EmailNotificationProperties {

    private String provider = "auto";
    private String from = "";
    private String fromName = "MegaStore";
    private String siteUrl = "http://localhost:8081";

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }
    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }
    public String getSiteUrl() { return siteUrl; }
    public void setSiteUrl(String siteUrl) { this.siteUrl = siteUrl; }
}
