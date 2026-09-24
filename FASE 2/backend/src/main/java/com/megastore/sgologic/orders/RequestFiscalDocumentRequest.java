package com.megastore.sgologic.orders;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record RequestFiscalDocumentRequest(
        @NotNull UUID paymentAttemptId,
        @NotBlank @Size(max = 32) String taxIdentifier,
        @NotBlank @Size(max = 200) String customerName,
        @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency
) {
}
