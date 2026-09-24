package com.megastore.sgologic.audit;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resuelve el actor de una operación auditable (AUD-01/AUD-02): el sujeto
 * autenticado del JWT o {@code system} cuando no hay contexto (trabajadores,
 * pruebas o búsquedas no autenticadas).
 */
@Component
class AuditActorProvider {

    String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return "system";
        }
        return authentication.getName();
    }
}
