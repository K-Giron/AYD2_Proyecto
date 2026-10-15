package com.megastore.sgologic.credit;

import com.megastore.sgologic.credit.api.CreditHistoryBatch;
import com.megastore.sgologic.credit.api.CreditHistoryEntry;
import com.megastore.sgologic.credit.api.CreditHistoryPort;
import com.megastore.sgologic.credit.api.CreditHistoryType;
import com.megastore.sgologic.credit.api.CreditOperationResult;
import com.megastore.sgologic.credit.api.CreditOperationStatus;
import com.megastore.sgologic.credit.api.CreditPort;
import com.megastore.sgologic.credit.api.CreditRequest;
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
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
class CreditLifecycleIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17.11-alpine3.23"))
            .withDatabaseName("sgo_credit_lifecycle_test")
            .withUsername("sgo_test")
            .withPassword("test_only_password");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired CreditPort credit;
    @Autowired CreditHistoryPort history;
    @Autowired CreditLifecycleService lifecycle;
    @Autowired JdbcTemplate jdbc;

    private UUID customer;

    /** Cliente con línea base de 2500.00, 25 % de devoluciones y sin mora. */
    @BeforeEach
    void eligibleCustomer() {
        customer = UUID.randomUUID();
        Instant base = Instant.now().minus(60, ChronoUnit.DAYS);
        List<CreditHistoryEntry> entries = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            entries.add(new CreditHistoryEntry(customer, "lc-p" + i + "-" + customer,
                    CreditHistoryType.PURCHASE, base.plus(i, ChronoUnit.DAYS), new BigDecimal("750.00"), null, false));
        }
        entries.add(new CreditHistoryEntry(customer, "lc-r1-" + customer, CreditHistoryType.RETURN,
                base.plus(10, ChronoUnit.DAYS), new BigDecimal("100.00"), null, false));
        history.ingest(new CreditHistoryBatch(CreditHistoryPort.CONTRACT_VERSION, UUID.randomUUID(), entries));
    }

    @Test
    void grantedCreditBecomesOverdueAfterItsDueDateAndBlocksNewCredit() {
        CreditOperationResult granted = grant("500.00");
        assertThat(available()).isEqualByComparingTo("2000.00");

        assertThat(lifecycle.markOverdue(granted.dueDate(), UUID.randomUUID())).isZero();
        assertThat(lifecycle.markOverdue(granted.dueDate().plusDays(1), UUID.randomUUID())).isGreaterThanOrEqualTo(1);

        assertThat(statusOf(granted)).isEqualTo(CreditOperationStatus.OVERDUE);
        assertThat(overdueFlag()).isTrue();
        CreditOperationResult blocked = request(UUID.randomUUID(), "100.00");
        assertThat(blocked.status()).isEqualTo(CreditOperationStatus.REJECTED);
        assertThat(blocked.publicReasonCode()).isEqualTo("OVERDUE_CREDIT");
    }

    @Test
    void settlingAnOverdueCreditReleasesTheLimitAndLiftsTheBlock() {
        CreditOperationResult granted = grant("500.00");
        lifecycle.markOverdue(LocalDate.now(ZoneOffset.UTC).plusDays(40), UUID.randomUUID());

        CreditOperationResult settled = lifecycle.settle(granted.creditOperationId(), UUID.randomUUID());

        assertThat(settled.status()).isEqualTo(CreditOperationStatus.SETTLED);
        assertThat(available()).isEqualByComparingTo("2500.00");
        assertThat(overdueFlag()).isFalse();
        assertThat(grant("100.00").status()).isEqualTo(CreditOperationStatus.GRANTED);
    }

    @Test
    void settlingTwiceReleasesTheLimitOnlyOnce() {
        CreditOperationResult granted = grant("500.00");

        lifecycle.settle(granted.creditOperationId(), UUID.randomUUID());
        CreditOperationResult again = lifecycle.settle(granted.creditOperationId(), UUID.randomUUID());

        assertThat(again.status()).isEqualTo(CreditOperationStatus.SETTLED);
        assertThat(available()).isEqualByComparingTo("2500.00");
    }

    @Test
    void concurrentSettlementsReleaseTheLimitOnlyOnce() throws Exception {
        CreditOperationResult granted = grant("500.00");
        var pool = Executors.newFixedThreadPool(4);
        var start = new CountDownLatch(1);
        List<Future<?>> runs = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            runs.add(pool.submit(() -> {
                start.await();
                return lifecycle.settle(granted.creditOperationId(), UUID.randomUUID());
            }));
        }
        start.countDown();
        for (Future<?> run : runs) run.get();
        pool.shutdown();

        assertThat(available()).isEqualByComparingTo("2500.00");
    }

    @Test
    void cancellingTheOrderVoidsTheObligationAndReturnsTheBalance() {
        UUID orderId = UUID.randomUUID();
        CreditOperationResult granted = grant(orderId, "500.00");

        var cancelled = lifecycle.cancelForOrder(orderId, UUID.randomUUID());
        lifecycle.cancelForOrder(orderId, UUID.randomUUID());

        assertThat(cancelled).isPresent();
        assertThat(cancelled.get().status()).isEqualTo(CreditOperationStatus.CANCELLED);
        assertThat(statusOf(granted)).isEqualTo(CreditOperationStatus.CANCELLED);
        assertThat(available()).isEqualByComparingTo("2500.00");
    }

    @Test
    void cancellingAnOrderWithoutCreditChangesNothing() {
        assertThat(lifecycle.cancelForOrder(UUID.randomUUID(), UUID.randomUUID())).isEmpty();
        assertThat(available()).isEqualByComparingTo("2500.00");
    }

    @Test
    void cancellingAnAlreadySettledCreditDoesNotReleaseItAgain() {
        UUID orderId = UUID.randomUUID();
        CreditOperationResult granted = grant(orderId, "500.00");
        lifecycle.settle(granted.creditOperationId(), UUID.randomUUID());

        lifecycle.cancelForOrder(orderId, UUID.randomUUID());

        assertThat(statusOf(granted)).isEqualTo(CreditOperationStatus.SETTLED);
        assertThat(available()).isEqualByComparingTo("2500.00");
    }

    @Test
    void rejectedOrUnknownOperationsCannotBeSettled() {
        UUID stranger = UUID.randomUUID();
        CreditOperationResult rejected = credit.requestCredit(new CreditRequest(UUID.randomUUID(), stranger,
                new BigDecimal("2500.01"), "GTQ", UUID.randomUUID(), "key-" + UUID.randomUUID()));

        assertThat(rejected.status()).isEqualTo(CreditOperationStatus.REJECTED);
        assertThatThrownBy(() -> lifecycle.settle(rejected.creditOperationId(), UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> lifecycle.settle(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(CreditOperationNotFoundException.class);
    }

    @Test
    void historyRebuildKeepsCountingTheOutstandingCredit() {
        grant("500.00");

        history.ingest(new CreditHistoryBatch(CreditHistoryPort.CONTRACT_VERSION, UUID.randomUUID(),
                List.of(new CreditHistoryEntry(customer, "lc-late-" + customer, CreditHistoryType.PURCHASE,
                        Instant.now().minusSeconds(60), new BigDecimal("750.00"), null, false))));

        // El cálculo histórico no reduce la línea base: 2500 menos 500 vigentes.
        assertThat(available()).isEqualByComparingTo("2000.00");
    }

    private CreditOperationResult grant(String amount) {
        return grant(UUID.randomUUID(), amount);
    }

    private CreditOperationResult grant(UUID orderId, String amount) {
        CreditOperationResult result = request(orderId, amount);
        assertThat(result.status()).isEqualTo(CreditOperationStatus.GRANTED);
        return result;
    }

    private CreditOperationResult request(UUID orderId, String amount) {
        return credit.requestCredit(new CreditRequest(orderId, customer,
                new BigDecimal(amount), "GTQ", UUID.randomUUID(), "key-" + UUID.randomUUID()));
    }

    private BigDecimal available() {
        return jdbc.queryForObject("SELECT available_credit FROM credit_customer_profiles WHERE customer_id = ?",
                BigDecimal.class, customer);
    }

    private boolean overdueFlag() {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT has_overdue_credit FROM credit_customer_profiles WHERE customer_id = ?",
                Boolean.class, customer));
    }

    private CreditOperationStatus statusOf(CreditOperationResult operation) {
        return CreditOperationStatus.valueOf(jdbc.queryForObject(
                "SELECT status FROM credit_operations WHERE id = ?", String.class, operation.creditOperationId()));
    }
}
