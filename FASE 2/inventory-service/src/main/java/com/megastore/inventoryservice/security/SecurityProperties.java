package com.megastore.inventoryservice.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Cuentas de demostración locales (ALMACEN, FINANZAS, LOGISTICA) y
 * configuración del JWT. No representan cuentas productivas.
 */
@Component
@ConfigurationProperties(prefix = "inventory.security")
public class SecurityProperties {

    private Jwt jwt = new Jwt();
    private List<User> users = new ArrayList<>();

    public Jwt getJwt() { return jwt; }
    public void setJwt(Jwt jwt) { this.jwt = jwt; }
    public List<User> getUsers() { return users; }
    public void setUsers(List<User> users) { this.users = users; }

    public static class Jwt {

        private String secret = "local_dev_only_inventory_jwt_secret_32b!";
        private String issuer = "inventory-service";
        private Duration ttl = Duration.ofHours(8);

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }
        public Duration getTtl() { return ttl; }
        public void setTtl(Duration ttl) { this.ttl = ttl; }
    }

    public static class User {

        private String username;
        private String password;
        private List<String> roles = new ArrayList<>();
        /** Solo cuentas TIENDA: el punto de venta por el que esa cuenta puede sincronizar. */
        private String pointOfSaleCode;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
        public String getPointOfSaleCode() { return pointOfSaleCode; }
        public void setPointOfSaleCode(String pointOfSaleCode) { this.pointOfSaleCode = pointOfSaleCode; }
    }
}
