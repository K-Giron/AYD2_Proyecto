package com.megastore.sgologic.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Configuración externalizada de seguridad (BB-07, ADR-013).
 *
 * <p>Los usuarios son cuentas de demostración locales definidas por
 * variables de entorno; no representan cuentas productivas. Las contraseñas
 * se codifican con BCrypt al arrancar y nunca se registran.</p>
 */
@Component
@ConfigurationProperties(prefix = "sgo.security")
public class SecurityProperties {

    private boolean enabled = true;
    private Jwt jwt = new Jwt();
    private List<User> users = new ArrayList<>();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Jwt getJwt() { return jwt; }
    public void setJwt(Jwt jwt) { this.jwt = jwt; }
    public List<User> getUsers() { return users; }
    public void setUsers(List<User> users) { this.users = users; }

    public static class Jwt {

        private String secret = "local_dev_only_jwt_secret_change_me_32b!";
        private String issuer = "sgo-logic";
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
        private UUID customerId;
        private List<String> roles = new ArrayList<>();

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public UUID getCustomerId() { return customerId; }
        public void setCustomerId(UUID customerId) { this.customerId = customerId; }
        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
    }
}
