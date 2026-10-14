package com.megastore.sgologic.credit.api;

public record CreditHistoryIngestResult(
        int received,
        int accepted,
        int duplicates,
        int profilesRebuilt
) {
}
