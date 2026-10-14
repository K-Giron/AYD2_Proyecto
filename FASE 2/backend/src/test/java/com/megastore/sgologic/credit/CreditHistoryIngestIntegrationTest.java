package com.megastore.sgologic.credit;

import com.megastore.sgologic.credit.api.CreditEvaluationRequest;
import com.megastore.sgologic.credit.api.CreditHistoryBatch;
import com.megastore.sgologic.credit.api.CreditHistoryEntry;
import com.megastore.sgologic.credit.api.CreditHistoryIngestResult;
import com.megastore.sgologic.credit.api.CreditHistoryPort;
import com.megastore.sgologic.credit.api.CreditHistoryType;
import com.megastore.sgologic.credit.api.CreditPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
class CreditHistoryIngestIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17.11-alpine3.23"))
            .withDatabaseName("sgo_credit_history_test")
            .withUsername("sgo_test")
            .withPassword("test_only_password");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired CreditHistoryPort history;
    @Autowired CreditPort credit;
    @Autowired JdbcTemplate jdbc;

    private UUID customer;

    @BeforeEach
    void newCustomer() {
        customer = UUID.randomUUID();
    }

    @Test
    void reprocessingTheSameBatchDoesNotDuplicateNorChangeTheProfile() {
        CreditHistoryBatch batch = batch(goodHistory("a"));

        CreditHistoryIngestResult first = history.ingest(batch);
        BigDecimal limitAfterFirst = availableCredit();
        CreditHistoryIngestResult second = history.ingest(batch);

        assertThat(first.accepted()).isEqualTo(7);
        assertThat(first.profilesRebuilt()).isEqualTo(1);
        assertThat(second.accepted()).isZero();
        assertThat(second.duplicates()).isEqualTo(7);
        assertThat(second.profilesRebuilt()).isZero();
        assertThat(count("credit_history_records")).isEqualTo(7);
        assertThat(availableCredit()).isEqualByComparingTo(limitAfterFirst);
    }

    @Test
    void projectedProfileDrivesTheEligibilityDecision() {
        history.ingest(batch(goodHistory("b")));

        var decision = credit.evaluate(new CreditEvaluationRequest(
                customer, new BigDecimal("300.00"), "GTQ", UUID.randomUUID()));

        assertThat(decision.eligible()).isTrue();
        assertThat(decision.approvedLimit()).isEqualByComparingTo("1200.00");
        String snapshot = jdbc.queryForObject("SELECT inputs_snapshot FROM credit_decisions WHERE id = ?",
                String.class, decision.decisionId());
        assertThat(snapshot).contains("\"completedPurchases\":4").contains("\"availableCredit\":1200.00");
    }

    @Test
    void historyOutsideTheWindowDoesNotCount() {
        Instant old = Instant.now().minus(500, ChronoUnit.DAYS);
        List<CreditHistoryEntry> entries = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            entries.add(entry("old-" + i, CreditHistoryType.PURCHASE, old, "1000.00", null, false));
        }
        entries.add(entry("recent-0", CreditHistoryType.PURCHASE, Instant.now().minusSeconds(60),
                "100.00", null, false));

        history.ingest(batch(entries));

        assertThat(jdbc.queryForObject("SELECT completed_purchases FROM credit_customer_profiles "
                + "WHERE customer_id = ?", Integer.class, customer)).isEqualTo(1);
        var decision = credit.evaluate(new CreditEvaluationRequest(
                customer, new BigDecimal("10.00"), "GTQ", UUID.randomUUID()));
        assertThat(decision.eligible()).isFalse();
        assertThat(decision.publicReasonCode()).isEqualTo("INSUFFICIENT_HISTORY");
    }

    @Test
    void newFactsRebuildTheProfileFromAllRecords() {
        history.ingest(batch(goodHistory("c")));
        history.ingest(batch(List.of(entry("c-late-payment", CreditHistoryType.PAYMENT,
                Instant.now().minusSeconds(30), "200.00", false, false))));

        BigDecimal rate = jdbc.queryForObject("SELECT on_time_payment_rate FROM credit_customer_profiles "
                + "WHERE customer_id = ?", BigDecimal.class, customer);
        assertThat(rate).isEqualByComparingTo("0.6667");
        assertThat(count("credit_history_records")).isEqualTo(8);
    }

    @Test
    void invalidBatchesAreRejectedWithoutPersistingAnything() {
        assertThatThrownBy(() -> history.ingest(new CreditHistoryBatch("credit-history-v9",
                UUID.randomUUID(), goodHistory("d")))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> history.ingest(batch(List.of(entry("d-pay", CreditHistoryType.PAYMENT,
                Instant.now().minusSeconds(10), "10.00", null, false)))))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(count("credit_history_records")).isZero();
    }

    private CreditHistoryBatch batch(List<CreditHistoryEntry> entries) {
        return new CreditHistoryBatch(CreditHistoryPort.CONTRACT_VERSION, UUID.randomUUID(), entries);
    }

    /** 4 compras de 750 (total 3000, límite 1200), 2 pagos puntuales y una devolución (25 %). */
    private List<CreditHistoryEntry> goodHistory(String prefix) {
        Instant base = Instant.now().minus(60, ChronoUnit.DAYS);
        return List.of(
                entry(prefix + "-p1", CreditHistoryType.PURCHASE, base, "750.00", null, false),
                entry(prefix + "-p2", CreditHistoryType.PURCHASE, base.plus(10, ChronoUnit.DAYS), "750.00", null, false),
                entry(prefix + "-p3", CreditHistoryType.PURCHASE, base.plus(20, ChronoUnit.DAYS), "750.00", null, false),
                entry(prefix + "-p4", CreditHistoryType.PURCHASE, base.plus(25, ChronoUnit.DAYS), "750.00", null, false),
                entry(prefix + "-pay1", CreditHistoryType.PAYMENT, base.plus(30, ChronoUnit.DAYS), "500.00", true, false),
                entry(prefix + "-pay2", CreditHistoryType.PAYMENT, base.plus(35, ChronoUnit.DAYS), "500.00", true, false),
                entry(prefix + "-ret1", CreditHistoryType.RETURN, base.plus(40, ChronoUnit.DAYS), "100.00", null, false));
    }

    private CreditHistoryEntry entry(String reference, CreditHistoryType type, Instant at, String amount,
                                     Boolean onTime, boolean critical) {
        return new CreditHistoryEntry(customer, reference + "-" + customer, type, at,
                new BigDecimal(amount), onTime, critical);
    }

    private BigDecimal availableCredit() {
        return jdbc.queryForObject("SELECT available_credit FROM credit_customer_profiles "
                + "WHERE customer_id = ?", BigDecimal.class, customer);
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE customer_id = ?",
                Integer.class, customer);
    }
}
