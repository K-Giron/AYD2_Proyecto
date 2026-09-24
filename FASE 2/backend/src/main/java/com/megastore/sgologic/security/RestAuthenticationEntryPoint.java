package com.megastore.sgologic.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Responde 401 con el formato común cuando falta un token válido. */
@Component
class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final RestSecurityErrorWriter errorWriter;

    RestAuthenticationEntryPoint(RestSecurityErrorWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        errorWriter.write(request, response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHENTICATED",
                "Se requiere un token de acceso válido para consultar este recurso.");
    }
}
