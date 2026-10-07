package com.logiexpress.carrier.shared;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "logiexpress")
public record LogiExpressProperties(@NotBlank String merchant, @NotBlank String apiKey) {
}
