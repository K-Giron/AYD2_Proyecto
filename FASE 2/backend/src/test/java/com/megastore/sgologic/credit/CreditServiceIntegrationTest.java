package com.megastore.sgologic.credit;

import com.megastore.sgologic.credit.api.CreditOperationResult;
import com.megastore.sgologic.credit.api.CreditOperationStatus;
import com.megastore.sgologic.credit.api.CreditPort;
import com.megastore.sgologic.credit.api.CreditRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
class CreditServiceIntegrationTest {

    private static final UUID ELIGIBLE_CUSTOMER =
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17.11-alpine3.23"))
            .withDatabaseName("sgo_credit_test")
            .withUsername("sgo_test")
            .withPassword("test_only_password");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired CreditPort credit;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void resetCreditLedger() {
        jdbc.update("DELETE FROM credit_operations");
        jdbc.update("DELETE FROM credit_decisions");
        jdbc.update("UPDATE credit_customer_profiles SET available_credit = 2500.00, version = 0 "
                + "WHERE customer_id = ?", ELIGIBLE_CUSTOMER);
    }

    @Test
    void concurrentEquivalentRequestsCreateOneCreditOperation() throws Exception {
        UUID orderId = UUID.randomUUID();
        String key = "credit-concurrent-" + UUID.randomUUID();
        CreditRequest request = request(orderId, ELIGIBLE_CUSTOMER, key);
        CountDownLatch start = new CountDownLatch(1);
        int decisionsBefore = jdbc.queryForObject(
                "SELECT count(*) FROM credit_decisions WHERE customer_id = ?",
                Integer.class, ELIGIBLE_CUSTOMER);

        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<CreditOperationResult> first = executor.submit(() -> requestAfter(start, request));
            Future<CreditOperationResult> second = executor.submit(() -> requestAfter(start, request));
            start.countDown();

            CreditOperationResult firstResult = first.get();
            CreditOperationResult secondResult = second.get();
            assertThat(secondResult.creditOperationId()).isEqualTo(firstResult.creditOperationId());
            assertThat(firstResult.status()).isEqualTo(CreditOperationStatus.GRANTED);
        }

        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM credit_operations WHERE order_id = ?", Integer.class, orderId))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM credit_decisions WHERE customer_id = ?",
                Integer.class, ELIGIBLE_CUSTOMER)).isEqualTo(decisionsBefore + 1);
    }

    @Test
    void repeatedRequestReturnsSameOperationWithoutAnotherDecision() {
        UUID orderId = UUID.randomUUID();
        String key = "credit-repeat-" + UUID.randomUUID();
        CreditRequest request = request(orderId, ELIGIBLE_CUSTOMER, key);

        CreditOperationResult first = credit.requestCredit(request);
        CreditOperationResult repeated = credit.requestCredit(request);

        assertThat(repeated.creditOperationId()).isEqualTo(first.creditOperationId());
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM credit_operations WHERE order_id = ?", Integer.class, orderId))
                .isEqualTo(1);
    }

    @Test
    void customerWithoutProfileIsRejectedWithoutDebtOrDueDate() {
        UUID orderId = UUID.randomUUID();
        CreditOperationResult result = credit.requestCredit(request(
                orderId, UUID.randomUUID(), "credit-rejected-" + UUID.randomUUID()));

        assertThat(result.status()).isEqualTo(CreditOperationStatus.REJECTED);
        assertThat(result.dueDate()).isNull();
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM credit_operations WHERE order_id = ? AND status = 'GRANTED'",
                Integer.class, orderId)).isZero();
    }

    @Test
    void reusedIdempotencyKeyForAnotherOrderIsRejectedWithoutCreatingAnotherOperation() {
        String key = "credit-key-conflict-" + UUID.randomUUID();
        CreditRequest original = request(UUID.randomUUID(), ELIGIBLE_CUSTOMER, key);
        credit.requestCredit(original);

        assertThatThrownBy(() -> credit.requestCredit(request(UUID.randomUUID(), ELIGIBLE_CUSTOMER, key)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("llave idempotente");
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM credit_operations WHERE idempotency_key = ?", Integer.class, key))
                .isEqualTo(1);
    }

    @Test
    void sameOrderWithAnotherKeyReturnsTheExistingOperation() {
        UUID orderId = UUID.randomUUID();
        CreditOperationResult original = credit.requestCredit(request(
                orderId, ELIGIBLE_CUSTOMER, "credit-order-first-" + UUID.randomUUID()));

        CreditOperationResult repeated = credit.requestCredit(request(
                orderId, ELIGIBLE_CUSTOMER, "credit-order-retry-" + UUID.randomUUID()));

        assertThat(repeated.creditOperationId()).isEqualTo(original.creditOperationId());
        assertThat(credit.latestForOrder(orderId)).contains(repeated);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM credit_operations WHERE order_id = ?", Integer.class, orderId))
                .isEqualTo(1);
    }

    @Test
    void grantedPurchasesConsumeTheSharedLimitAndTheNextPurchaseCannotExceedTheBalance() {
        CreditOperationResult first = credit.requestCredit(request(
                UUID.randomUUID(), ELIGIBLE_CUSTOMER, "credit-balance-first-" + UUID.randomUUID(), "1800.00"));
        CreditOperationResult second = credit.requestCredit(request(
                UUID.randomUUID(), ELIGIBLE_CUSTOMER, "credit-balance-second-" + UUID.randomUUID(), "800.00"));

        assertThat(first.status()).isEqualTo(CreditOperationStatus.GRANTED);
        assertThat(second.status()).isEqualTo(CreditOperationStatus.REJECTED);
        assertThat(second.publicReasonCode()).isEqualTo("INSUFFICIENT_LIMIT");
        assertThat(jdbc.queryForObject(
                "SELECT available_credit FROM credit_customer_profiles WHERE customer_id = ?",
                BigDecimal.class, ELIGIBLE_CUSTOMER)).isEqualByComparingTo("700.00");
        assertThat(jdbc.queryForObject(
                "SELECT coalesce(sum(amount), 0) FROM credit_operations "
                        + "WHERE customer_id = ? AND status = 'GRANTED'",
                BigDecimal.class, ELIGIBLE_CUSTOMER)).isEqualByComparingTo("1800.00");
    }

    @Test
    void concurrentDifferentOrdersCannotSpendTheSameCustomerLimitTwice() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        CreditRequest firstRequest = request(UUID.randomUUID(), ELIGIBLE_CUSTOMER,
                "credit-shared-limit-a-" + UUID.randomUUID(), "1600.00");
        CreditRequest secondRequest = request(UUID.randomUUID(), ELIGIBLE_CUSTOMER,
                "credit-shared-limit-b-" + UUID.randomUUID(), "1600.00");

        CreditOperationResult first;
        CreditOperationResult second;
        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<CreditOperationResult> firstFuture = executor.submit(() -> requestAfter(start, firstRequest));
            Future<CreditOperationResult> secondFuture = executor.submit(() -> requestAfter(start, secondRequest));
            start.countDown();
            first = firstFuture.get();
            second = secondFuture.get();
        }

        assertThat(java.util.List.of(first.status(), second.status()))
                .containsExactlyInAnyOrder(CreditOperationStatus.GRANTED, CreditOperationStatus.REJECTED);
        assertThat(jdbc.queryForObject(
                "SELECT available_credit FROM credit_customer_profiles WHERE customer_id = ?",
                BigDecimal.class, ELIGIBLE_CUSTOMER)).isEqualByComparingTo("900.00");
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM credit_operations WHERE customer_id = ? AND status = 'GRANTED'",
                Integer.class, ELIGIBLE_CUSTOMER)).isEqualTo(1);
    }

    private CreditOperationResult requestAfter(CountDownLatch start, CreditRequest request) throws Exception {
        start.await();
        return credit.requestCredit(request);
    }

    private CreditRequest request(UUID orderId, UUID customerId, String key) {
        return request(orderId, customerId, key, "399.00");
    }

    private CreditRequest request(UUID orderId, UUID customerId, String key, String amount) {
        return new CreditRequest(orderId, customerId, new BigDecimal(amount), "GTQ",
                UUID.randomUUID(), key);
    }
}
