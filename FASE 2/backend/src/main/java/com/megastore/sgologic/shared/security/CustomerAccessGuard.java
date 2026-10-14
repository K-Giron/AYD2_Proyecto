package com.megastore.sgologic.shared.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Aplica propiedad por cliente usando la identidad firmada del JWT.
 * Los roles internos operan por sus permisos; un CLIENT solo accede a su UUID.
 */
@Component
public class CustomerAccessGuard {

    private final boolean securityEnabled;

    public CustomerAccessGuard(@Value("${sgo.security.enabled:true}") boolean securityEnabled) {
        this.securityEnabled = securityEnabled;
    }

    public void requireAccess(Authentication authentication, UUID requestedCustomerId) {
        if (!securityEnabled) return;
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("No existe una identidad autenticada.");
        }
        if (!isClient(authentication)) return;
        UUID authenticatedCustomerId = authenticatedCustomerId(authentication);
        if (!authenticatedCustomerId.equals(requestedCustomerId)) {
            throw new AccessDeniedException("El recurso pertenece a otro cliente.");
        }
    }

    /** Fuerza que una consulta CLIENT use el UUID firmado, aunque omita el filtro. */
    public UUID constrainCustomerFilter(Authentication authentication, UUID requestedCustomerId) {
        if (!securityEnabled || !isClient(authentication)) return requestedCustomerId;
        UUID authenticatedCustomerId = authenticatedCustomerId(authentication);
        if (requestedCustomerId != null && !authenticatedCustomerId.equals(requestedCustomerId)) {
            throw new AccessDeniedException("No puede consultar información de otro cliente.");
        }
        return authenticatedCustomerId;
    }

    private boolean isClient(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_CLIENT".equals(authority.getAuthority()));
    }

    private UUID authenticatedCustomerId(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            throw new AccessDeniedException("La identidad del cliente no proviene de un JWT válido.");
        }
        String claim = jwtAuthentication.getToken().getClaimAsString("customer_id");
        if (claim == null || claim.isBlank()) {
            throw new AccessDeniedException("El JWT no identifica al cliente.");
        }
        try {
            return UUID.fromString(claim);
        } catch (IllegalArgumentException malformedClaim) {
            throw new AccessDeniedException("El JWT contiene una identidad de cliente inválida.");
        }
    }
}
