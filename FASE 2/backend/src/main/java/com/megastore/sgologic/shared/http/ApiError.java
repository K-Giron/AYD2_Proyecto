package com.megastore.sgologic.shared.http;

import java.time.Instant;

public record ApiError(
        String code,
        String message,
        Instant timestamp,
        String path,
        String correlationId
) {
}
