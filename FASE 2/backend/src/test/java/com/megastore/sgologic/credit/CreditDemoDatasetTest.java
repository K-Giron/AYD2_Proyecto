package com.megastore.sgologic.credit;

import com.megastore.sgologic.credit.api.CreditEvaluationRequest;
import com.megastore.sgologic.credit.api.CreditHistoryBatch;
import com.megastore.sgologic.credit.api.CreditHistoryEntry;
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

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que el dataset de demostración (tests/data/credit-history-demo.csv)
 * produzca la decisión que documenta el Manual de Usuario para cada cliente.
 */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
class CreditDemoDatasetTest {

    private static final Path DATASET = Path.of("..", "tests", "data", "credit-history-demo.csv");

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17.11-alpine3.23"))
            .withDatabaseName("sgo_credit_demo_test")
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

    @BeforeEach
    void cleanDemoCustomers() {
        jdbc.update("DELETE FROM credit_history_records WHERE source_reference LIKE 'demo-cr%'");
        jdbc.update("DELETE FROM credit_customer_profiles WHERE customer_id::text LIKE 'bbbbbbbb-%'");
    }

    @Test
    void eachDemoCustomerGetsTheDocumentedDecision() throws IOException {
        var result = history.ingest(new CreditHistoryBatch(CreditHistoryPort.CONTRACT_VERSION,
                UUID.randomUUID(), readDataset()));

        assertThat(result.profilesRebuilt()).isEqualTo(5);
        assertDecision("000000000001", true, "ELIGIBLE");
        assertDecision("000000000002", true, "ELIGIBLE");
        assertDecision("000000000003", false, "HIGH_RETURN_RATE");
        assertDecision("000000000004", false, "CRITICAL_INCIDENTS");
        assertDecision("000000000005", false, "LATE_PAYMENT_HISTORY");
    }

    @Test
    void loadingTheDatasetTwiceChangesNothing() throws IOException {
        List<CreditHistoryEntry> entries = readDataset();
        history.ingest(new CreditHistoryBatch(CreditHistoryPort.CONTRACT_VERSION, UUID.randomUUID(), entries));

        var second = history.ingest(new CreditHistoryBatch(CreditHistoryPort.CONTRACT_VERSION,
                UUID.randomUUID(), entries));

        assertThat(second.accepted()).isZero();
        assertThat(second.duplicates()).isEqualTo(entries.size());
    }

    private void assertDecision(String customerSuffix, boolean eligible, String reason) {
        var decision = credit.evaluate(new CreditEvaluationRequest(
                UUID.fromString("bbbbbbbb-0000-4000-8000-" + customerSuffix),
                new BigDecimal("500.00"), "GTQ", UUID.randomUUID()));
        assertThat(decision.eligible()).as(customerSuffix).isEqualTo(eligible);
        assertThat(decision.publicReasonCode()).as(customerSuffix).isEqualTo(reason);
    }

    private static List<CreditHistoryEntry> readDataset() throws IOException {
        Instant now = Instant.now();
        return Files.readAllLines(DATASET).stream().skip(1).filter(line -> !line.isBlank())
                .map(line -> line.strip().split(",", -1))
                .map(c -> new CreditHistoryEntry(UUID.fromString(c[0]), c[1], CreditHistoryType.valueOf(c[2]),
                        now.minus(Long.parseLong(c[3]), ChronoUnit.DAYS), new BigDecimal(c[4]),
                        c[5].isEmpty() ? null : Boolean.valueOf(c[5]), Boolean.parseBoolean(c[6])))
                .toList();
    }
}
