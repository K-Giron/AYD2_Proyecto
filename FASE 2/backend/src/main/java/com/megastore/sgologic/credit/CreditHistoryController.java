package com.megastore.sgologic.credit;

import com.megastore.sgologic.credit.api.CreditHistoryBatch;
import com.megastore.sgologic.credit.api.CreditHistoryEntry;
import com.megastore.sgologic.credit.api.CreditHistoryIngestResult;
import com.megastore.sgologic.credit.api.CreditHistoryPort;
import com.megastore.sgologic.credit.api.CreditHistoryType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Entrada del historial ERP (credit-history-v1); el rol FINANCE lo fija SecurityConfig. */
@RestController
@RequestMapping("/api/v1/credit/history")
class CreditHistoryController {

    private final CreditHistoryPort history;

    CreditHistoryController(CreditHistoryPort history) {
        this.history = history;
    }

    @PostMapping
    ResponseEntity<CreditHistoryIngestResult> ingest(@Valid @RequestBody HistoryBatchRequest body) {
        List<CreditHistoryEntry> entries = body.entries().stream()
                .map(e -> new CreditHistoryEntry(e.customerId(), e.sourceReference(), e.type(),
                        e.occurredAt(), e.amount(), e.onTime(), e.critical()))
                .toList();
        return ResponseEntity.ok(history.ingest(new CreditHistoryBatch(
                body.contractVersion(), body.correlationId(), entries)));
    }

    record HistoryBatchRequest(@NotNull String contractVersion, @NotNull UUID correlationId,
                               @NotEmpty List<@Valid @NotNull HistoryEntryRequest> entries) {
    }

    record HistoryEntryRequest(@NotNull UUID customerId, @NotNull String sourceReference,
                               @NotNull CreditHistoryType type, @NotNull Instant occurredAt,
                               BigDecimal amount, Boolean onTime, boolean critical) {
    }
}
