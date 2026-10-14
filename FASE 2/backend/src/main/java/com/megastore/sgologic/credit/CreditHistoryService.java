package com.megastore.sgologic.credit;

import com.megastore.sgologic.audit.api.AuditEntryRequest;
import com.megastore.sgologic.audit.api.AuditRecorder;
import com.megastore.sgologic.credit.CreditProfileCalculator.HistoryAggregates;
import com.megastore.sgologic.credit.CreditProfileCalculator.ProfileValues;
import com.megastore.sgologic.credit.api.CreditHistoryBatch;
import com.megastore.sgologic.credit.api.CreditHistoryEntry;
import com.megastore.sgologic.credit.api.CreditHistoryIngestResult;
import com.megastore.sgologic.credit.api.CreditHistoryPort;
import com.megastore.sgologic.credit.api.CreditHistoryType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Proyección local del historial del ERP. La idempotencia la garantiza la
 * restricción única de {@code source_reference}; el perfil siempre se
 * reconstruye desde los hechos, nunca se acumula de forma incremental.
 */
@Service
class CreditHistoryService implements CreditHistoryPort {

    static final int MAX_ENTRIES_PER_BATCH = 1000;
    private static final int MAX_REFERENCE_LENGTH = 160;

    private final JdbcTemplate jdbc;
    private final CreditProperties properties;
    private final CreditProfileCalculator calculator;
    private final CreditConcurrencyGuard concurrency;
    private final AuditRecorder audit;
    private final Clock clock;

    CreditHistoryService(JdbcTemplate jdbc, CreditProperties properties, CreditProfileCalculator calculator,
                         CreditConcurrencyGuard concurrency, AuditRecorder audit, Clock clock) {
        this.jdbc = jdbc;
        this.properties = properties;
        this.calculator = calculator;
        this.concurrency = concurrency;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CreditHistoryIngestResult ingest(CreditHistoryBatch batch) {
        validate(batch);
        Instant now = clock.instant();
        int accepted = 0;
        Set<UUID> affected = new TreeSet<>();
        for (CreditHistoryEntry entry : batch.entries()) {
            if (insertIfNew(entry, batch.contractVersion(), now)) {
                accepted++;
                affected.add(entry.customerId());
            }
        }
        for (UUID customerId : affected) {
            concurrency.lockCustomer(customerId);
            rebuildProfile(customerId, now, batch.correlationId());
        }
        return new CreditHistoryIngestResult(batch.entries().size(), accepted,
                batch.entries().size() - accepted, affected.size());
    }

    private boolean insertIfNew(CreditHistoryEntry entry, String contractVersion, Instant now) {
        int rows = jdbc.update("""
                INSERT INTO credit_history_records (id, customer_id, source_reference, record_type,
                    occurred_at, amount, on_time, critical, contract_version, ingested_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (source_reference) DO NOTHING
                """, UUID.randomUUID(), entry.customerId(), entry.sourceReference(), entry.type().name(),
                Timestamp.from(entry.occurredAt()), entry.amount() == null ? BigDecimal.ZERO : entry.amount(),
                entry.onTime(), entry.critical(), contractVersion, Timestamp.from(now));
        return rows == 1;
    }

    private void rebuildProfile(UUID customerId, Instant now, UUID correlationId) {
        Instant windowStart = now.atZone(ZoneOffset.UTC)
                .minusMonths(properties.getHistoryWindowMonths()).toInstant();
        BigDecimal outstanding = outstandingCredit(customerId);
        HistoryAggregates aggregates = jdbc.queryForObject("""
                SELECT count(*) FILTER (WHERE record_type = 'PURCHASE') AS purchases,
                       coalesce(sum(amount) FILTER (WHERE record_type = 'PURCHASE'), 0) AS purchase_total,
                       count(*) FILTER (WHERE record_type = 'PAYMENT') AS payments,
                       count(*) FILTER (WHERE record_type = 'PAYMENT' AND on_time) AS on_time_payments,
                       count(*) FILTER (WHERE record_type = 'RETURN') AS returns,
                       count(*) FILTER (WHERE record_type = 'INCIDENT' AND critical) AS critical_incidents
                FROM credit_history_records
                WHERE customer_id = ? AND occurred_at >= ?
                """, (rs, row) -> new HistoryAggregates(rs.getInt("purchases"),
                        rs.getBigDecimal("purchase_total"), rs.getInt("payments"),
                        rs.getInt("on_time_payments"), rs.getInt("returns"),
                        rs.getInt("critical_incidents"), outstanding),
                customerId, Timestamp.from(windowStart));
        ProfileValues values = calculator.calculate(aggregates);
        jdbc.update("""
                INSERT INTO credit_customer_profiles (customer_id, completed_purchases, on_time_payment_rate,
                    return_rate, critical_incidents, has_overdue_credit, available_credit, observed_at, version)
                VALUES (?, ?, ?, ?, ?, FALSE, ?, ?, 0)
                ON CONFLICT (customer_id) DO UPDATE SET
                    completed_purchases = EXCLUDED.completed_purchases,
                    on_time_payment_rate = EXCLUDED.on_time_payment_rate,
                    return_rate = EXCLUDED.return_rate,
                    critical_incidents = EXCLUDED.critical_incidents,
                    available_credit = EXCLUDED.available_credit,
                    observed_at = EXCLUDED.observed_at,
                    version = credit_customer_profiles.version + 1
                """, customerId, values.completedPurchases(), values.onTimePaymentRate(), values.returnRate(),
                values.criticalIncidents(), values.availableCredit(), Timestamp.from(now));
        audit.record(new AuditEntryRequest("CREDIT_PROFILE", customerId, null, "CREDIT_PROFILE_REBUILT",
                "OK", null,
                "{\"completedPurchases\":" + values.completedPurchases()
                        + ",\"criticalIncidents\":" + values.criticalIncidents()
                        + ",\"ruleVersion\":\"" + properties.getRuleVersion() + "\"}",
                correlationId, correlationId, null));
    }

    private BigDecimal outstandingCredit(UUID customerId) {
        return jdbc.queryForObject("""
                SELECT coalesce(sum(amount), 0) FROM credit_operations
                WHERE customer_id = ? AND status = 'GRANTED'
                """, BigDecimal.class, customerId);
    }

    private void validate(CreditHistoryBatch batch) {
        if (batch == null || batch.correlationId() == null) {
            throw new IllegalArgumentException("correlationId es obligatorio.");
        }
        if (!CONTRACT_VERSION.equals(batch.contractVersion())) {
            throw new IllegalArgumentException("Versión de contrato no soportada: " + batch.contractVersion());
        }
        if (batch.entries() == null || batch.entries().isEmpty()
                || batch.entries().size() > MAX_ENTRIES_PER_BATCH) {
            throw new IllegalArgumentException(
                    "El lote debe traer entre 1 y " + MAX_ENTRIES_PER_BATCH + " registros.");
        }
        Set<String> references = new HashSet<>();
        Instant now = clock.instant();
        for (CreditHistoryEntry entry : batch.entries()) {
            if (entry == null || entry.customerId() == null || entry.type() == null
                    || entry.occurredAt() == null) {
                throw new IllegalArgumentException("Cada registro requiere customerId, type y occurredAt.");
            }
            if (entry.sourceReference() == null || entry.sourceReference().isBlank()
                    || entry.sourceReference().length() > MAX_REFERENCE_LENGTH) {
                throw new IllegalArgumentException("sourceReference es obligatorio y admite hasta "
                        + MAX_REFERENCE_LENGTH + " caracteres.");
            }
            if (!references.add(entry.sourceReference())) {
                throw new IllegalArgumentException(
                        "sourceReference repetido en el lote: " + entry.sourceReference());
            }
            if (entry.amount() != null && entry.amount().signum() < 0) {
                throw new IllegalArgumentException("El monto no puede ser negativo.");
            }
            if (entry.type() == CreditHistoryType.PURCHASE
                    && (entry.amount() == null || entry.amount().signum() <= 0)) {
                throw new IllegalArgumentException("Una compra requiere un monto mayor que cero.");
            }
            if (entry.type() == CreditHistoryType.PAYMENT && entry.onTime() == null) {
                throw new IllegalArgumentException("Un pago requiere indicar onTime.");
            }
            if (entry.occurredAt().isAfter(now.plusSeconds(300))) {
                throw new IllegalArgumentException("occurredAt no puede estar en el futuro.");
            }
        }
    }
}
