package com.megastore.sgologic.customers;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterCustomerRequest(
        @NotBlank @Size(max = 160) String username,
        @NotBlank @Size(max = 160) String fullName,
        @Pattern(regexp = "WEB|MOBILE|STORE|CALL_CENTER") String channel
) {
}
