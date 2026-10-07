package com.megastore.inventoryservice.security;

import com.megastore.inventoryservice.shared.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;

/** Escribe las respuestas 401/403 en el formato común de error ({@code ApiError}). */
@Component
class RestSecurityErrorWriter {

    private final ObjectMapper objectMapper;

    RestSecurityErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    void write(HttpServletRequest request, HttpServletResponse response, int status,
               String code, String message) throws IOException {
        ApiError error = new ApiError(code, message, Instant.now(), request.getRequestURI());
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
