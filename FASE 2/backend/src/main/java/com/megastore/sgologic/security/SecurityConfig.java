package com.megastore.sgologic.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Seguridad del MVP (BB-07, ADR-013): API sin estado con JWT HMAC-SHA256,
 * roles por endpoint y cuentas de demostración configurables por entorno.
 *
 * <p>Con {@code sgo.security.enabled=false} (perfil de pruebas) la cadena
 * permite todo, de modo que las pruebas de negocio no necesitan token.</p>
 */
@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityProperties properties,
                                            RestAuthenticationEntryPoint authenticationEntryPoint,
                                            RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(registry -> {
                    if (!properties.isEnabled()) {
                        registry.anyRequest().permitAll();
                        return;
                    }
                    registry
                            .requestMatchers("/api/v1/auth/login", "/actuator/health", "/actuator/health/**",
                                    "/actuator/info", "/api/v1/platform/**")
                            .permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/v1/inventory/stock",
                                    "/api/v1/online-catalog", "/api/v1/online-catalog/**")
                            .permitAll()
                            // Administración de la tienda online (onlinestore, ADR-015).
                            // La consola interna también consulta el inventario y el kárdex de la tienda online.
                            .requestMatchers(HttpMethod.GET, "/api/v1/online-store/inventory",
                                    "/api/v1/online-store/movements")
                            .hasAnyRole("ONLINE_STORE", "SUPPORT", "LOGISTICS", "FINANCE")
                            // El almacén central entrega aquí los eventos de stock de POS-ONLINE (cuenta de servicio).
                            .requestMatchers(HttpMethod.POST, "/api/v1/online-store/stock-events")
                            .hasRole("POS_EVENTS")
                            .requestMatchers("/api/v1/online-store/**")
                            .hasRole("ONLINE_STORE")
                            // Factura del cliente (CUI, serie, XML), sin datos internos del documento fiscal.
                            .requestMatchers(HttpMethod.GET, "/api/v1/invoices/orders/*")
                            .hasAnyRole("CLIENT", "FINANCE", "ONLINE_STORE")
                            .requestMatchers("/api/v1/reports/**")
                            .hasRole("FINANCE")
                            .requestMatchers("/api/v1/fiscal-documents/**", "/api/v1/integrations/erp/**",
                                    "/api/v1/simulators/**", "/api/v1/audit/**")
                            .hasRole("FINANCE")
                            // El seguimiento es visible para el cliente (CDU-09); solo
                            // Logística crea despachos, guías y cambios de método.
                            // La tienda de retiro confirma la entrega al cliente (sistema de tienda, ADR-015).
                            .requestMatchers(HttpMethod.POST, "/api/v1/store-pickups/*/handoff")
                            .hasRole("STORE")
                            .requestMatchers(HttpMethod.GET, "/api/v1/shipments/**")
                            .hasAnyRole("CLIENT", "SUPPORT", "LOGISTICS")
                            .requestMatchers("/api/v1/shipments/**", "/api/v1/integrations/pos/**")
                            .hasRole("LOGISTICS")
                            .requestMatchers(HttpMethod.GET, "/api/v1/notifications/**")
                            .hasAnyRole("CLIENT", "SUPPORT")
                            .requestMatchers(HttpMethod.POST, "/api/v1/notifications")
                            .hasRole("SUPPORT")
                            .requestMatchers(HttpMethod.GET, "/api/v1/incidents/**")
                            .hasAnyRole("CLIENT", "SUPPORT")
                            .requestMatchers(HttpMethod.POST, "/api/v1/incidents")
                            .hasAnyRole("CLIENT", "SUPPORT")
                            .requestMatchers(HttpMethod.PATCH, "/api/v1/incidents/**")
                            .hasRole("SUPPORT")
                            .requestMatchers("/api/v1/carts/**", "/api/v1/orders/**")
                            .hasAnyRole("CLIENT", "SUPPORT", "LOGISTICS")
                            .requestMatchers("/api/v1/payments/**")
                            .hasAnyRole("CLIENT", "SUPPORT")
                            .requestMatchers("/api/v1/inventory/**")
                            .hasAnyRole("CLIENT", "SUPPORT", "LOGISTICS")
                            .requestMatchers("/api/v1/query/**")
                            .hasAnyRole("SUPPORT", "LOGISTICS")
                            .anyRequest().authenticated();
                });
        if (properties.isEnabled()) {
            http.oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        }
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(SecurityProperties properties, PasswordEncoder passwordEncoder) {
        List<UserDetails> users = properties.getUsers().stream()
                .map(user -> User.withUsername(user.getUsername())
                        .password(passwordEncoder.encode(user.getPassword()))
                        .roles(user.getRoles().toArray(String[]::new))
                        .build())
                .toList();
        return new InMemoryUserDetailsManager(users);
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    JwtDecoder jwtDecoder(SecurityProperties properties) {
        return NimbusJwtDecoder.withSecretKey(secretKey(properties)).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    JwtEncoder jwtEncoder(SecurityProperties properties) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey(properties)));
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    private SecretKeySpec secretKey(SecurityProperties properties) {
        byte[] secret = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(secret, "HmacSHA256");
    }
}
