package com.payklik.gateway.shared;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "payklik")
public record PayKlikProperties(@NotBlank String merchant, @NotBlank String apiKey) {
}
