package com.megastore.sgologic.fiscal;

import java.util.UUID;

public record FiscalProcessingResult(
        UUID documentId,
        UUID orderId,
        FiscalStatus status,
        String cui,
        String series,
        String satAuthorization,
        String failureCode
) {
}
