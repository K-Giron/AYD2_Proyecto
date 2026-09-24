package com.megastore.sgologic.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Responde 403 con el formato común cuando el rol no autoriza el recurso. */
@Component
class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final RestSecurityErrorWriter errorWriter;

    RestAccessDeniedHandler(RestSecurityErrorWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        errorWriter.write(request, response, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN",
                "El rol del token no tiene autorización para este recurso.");
    }
}
