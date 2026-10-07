package com.megastore.storeservice.shared;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "store")
public record StoreProperties(String code, String name, Invoice invoice, Remote central, Remote monolith, Sync sync) {

    /**
     * Datos del emisor que salen impresos en la factura y zona horaria de la
     * tienda (las ventas "de hoy" se cuentan en hora local, no en UTC).
     */
    public record Invoice(String legalName, String taxId, String address, String timezone) {
    }

    /** Sistema remoto y la cuenta de equipo con la que esta tienda se autentica en él. */
    public record Remote(String url, String username, String password) {
    }

    public record Sync(Duration pushInterval, Duration pullInterval, boolean enabled) {
    }
}
