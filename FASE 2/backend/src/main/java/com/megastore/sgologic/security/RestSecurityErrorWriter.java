package com.megastore.sgologic.security;

import com.megastore.sgologic.shared.http.ApiError;
import com.megastore.sgologic.shared.http.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;

/**
 * Escribe las respuestas 401/403 en el formato común de error
 * ({@code ApiError}) con el correlation ID de la petición y registra el
 * evento de seguridad con la ruta enmascarada.
 */
@Component
class RestSecurityErrorWriter {

    private static final Logger log = LoggerFactory.getLogger(RestSecurityErrorWriter.class);

    private final ObjectMapper objectMapper;
    private final SensitiveDataSanitizer sanitizer;

    RestSecurityErrorWriter(ObjectMapper objectMapper, SensitiveDataSanitizer sanitizer) {
        this.objectMapper = objectMapper;
        this.sanitizer = sanitizer;
    }

    void write(HttpServletRequest request, HttpServletResponse response, int status,
               String code, String message) throws IOException {
        String correlationId = String.valueOf(request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME));
        ApiError error = new ApiError(code, message, Instant.now(), request.getRequestURI(), correlationId);
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), error);
        log.warn("Seguridad: {} {} -> {} ({})", request.getMethod(),
                sanitizer.mask(request.getRequestURI()), status, correlationId);
    }
}
