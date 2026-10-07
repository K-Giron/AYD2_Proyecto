package com.megastore.sgologic.onlinestore;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Conexión con el almacén central con la cuenta de equipo de la tienda
 * online (rol TIENDA ligado a POS-ONLINE), y la ubicación de inventario
 * donde entra lo recibido.
 */
@Component
@ConfigurationProperties(prefix = "sgo.integrations.online-store")
public class OnlineStoreProperties {

    private String baseUrl = "http://inventory-service:8080";
    private String username = "tienda-online@almacencentral.local";
    private String password = "TiendaOnline123";
    private String pointOfSaleCode = "POS-ONLINE";
    private String locationCode = "CD-WEB";
    private long timeoutMs = 5000L;
    /** Dónde puede recoger el cliente su compra (solo ubicaciones físicas activas). */
    private List<PickupPoint> pickupPoints = new ArrayList<>();

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPointOfSaleCode() { return pointOfSaleCode; }
    public void setPointOfSaleCode(String pointOfSaleCode) { this.pointOfSaleCode = pointOfSaleCode; }
    public String getLocationCode() { return locationCode; }
    public void setLocationCode(String locationCode) { this.locationCode = locationCode; }
    public long getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; }
    public List<PickupPoint> getPickupPoints() { return pickupPoints; }
    public void setPickupPoints(List<PickupPoint> pickupPoints) { this.pickupPoints = pickupPoints; }

    public static class PickupPoint {
        private String code;
        private String name;
        private String address;
        private String hours;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getHours() { return hours; }
        public void setHours(String hours) { this.hours = hours; }
    }
}
