package com.megastore.sgologic.credit.api;

import java.util.List;
import java.util.UUID;

public record CreditHistoryBatch(
        String contractVersion,
        UUID correlationId,
        List<CreditHistoryEntry> entries
) {
}
