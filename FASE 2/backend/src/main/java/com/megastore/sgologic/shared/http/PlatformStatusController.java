package com.megastore.sgologic.shared.http;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/platform")
public class PlatformStatusController {

    private final String applicationName;

    public PlatformStatusController(@Value("${spring.application.name}") String applicationName) {
        this.applicationName = applicationName;
    }

    @GetMapping("/status")
    public PlatformStatus status(HttpServletRequest request) {
        return new PlatformStatus(
                applicationName,
                "UP",
                Instant.now(),
                request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME).toString()
        );
    }

    public record PlatformStatus(String applicationName, String status, Instant timestamp, String correlationId) {
    }
}
