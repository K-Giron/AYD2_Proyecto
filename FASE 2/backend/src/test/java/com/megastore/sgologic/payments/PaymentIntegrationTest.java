package com.megastore.sgologic.payments;

import com.megastore.sgologic.payments.api.PaymentMethod;

import com.megastore.sgologic.payments.api.PaymentPort;
import com.megastore.sgologic.payments.api.PaymentRequest;
import com.megastore.sgologic.payments.api.PaymentResult;
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
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PAY-01 contra PostgreSQL y RabbitMQ reales: intento persistido con orden,
 * canal, fecha, referencia y resultado; idempotencia (incluida la
 * concurrencia), evidencia auditable y ausencia de PAN/CVV.
 */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PaymentIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17.11-alpine3.23"))
            .withDatabaseName("sgo_logic_test")
            .withUsername("sgo_test")
            .withPassword("test_only_password");

    @Container
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:4.3.5-management-alpine"));

    @DynamicPropertySource
    static void infrastructureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);
    }

    @Autowired
    PaymentPort paymentPort;

    @Autowired
    PaymentAttemptService paymentService;

    @Autowired
    PaymentAttemptRepository repository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void approvedPaymentPersistsAttemptAndAuditEvidence() {
        UUID orderId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();

        PaymentResult result = paymentPort.requestPayment(request(orderId, "it-pay-1", correlationId));

        assertThat(result.status()).isEqualTo("APPROVED");
        assertThat(result.providerReference()).isNotBlank();

        PaymentAttempt attempt = repository.findById(result.paymentAttemptId()).orElseThrow();
        assertThat(attempt.getOrderId()).isEqualTo(orderId);
        assertThat(attempt.getChannel()).isEqualTo(PaymentChannel.MOBILE);
        assertThat(attempt.getMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(attempt.getAmount()).isEqualByComparingTo("150.00");
        assertThat(attempt.getCurrency()).isEqualTo("GTQ");
        assertThat(attempt.getCorrelationId()).isEqualTo(correlationId);
        assertThat(attempt.getProviderAttemptId()).isNotNull();
        assertThat(attempt.getCreatedAt()).isNotNull();

        Integer auditRows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_events WHERE aggregate_id = ? "
                        + "AND action IN ('PAYMENT_ATTEMPT_CREATED', 'PAYMENT_APPROVED')",
                Integer.class, attempt.getId());
        assertThat(auditRows).isEqualTo(2);

        String providerReference = jdbcTemplate.queryForObject(
                "SELECT provider_reference FROM payment_attempts WHERE id = ?", String.class, attempt.getId());
        assertThat(providerReference).doesNotContainPattern("\\d{13,}");
        assertThat(providerReference.toLowerCase()).doesNotContain("cvv");
    }

    @Test
    void repeatedIdempotencyKeyDoesNotCreateAnotherAttempt() {
        UUID orderId = UUID.randomUUID();

        PaymentResult first = paymentPort.requestPayment(request(orderId, "it-pay-2", UUID.randomUUID()));
        PaymentResult second = paymentPort.requestPayment(request(orderId, "it-pay-2", UUID.randomUUID()));

        assertThat(second.paymentAttemptId()).isEqualTo(first.paymentAttemptId());
        assertThat(repository.findByOrderIdOrderByCreatedAtDesc(orderId)).hasSize(1);
    }

    @Test
    void concurrentSameIdempotencyKeyCreatesOneAttempt() throws Exception {
        UUID orderId = UUID.randomUUID();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<PaymentResult>> futures = List.of(
                    pool.submit(() -> payAfter(start, orderId, "it-pay-3")),
                    pool.submit(() -> payAfter(start, orderId, "it-pay-3")));
            start.countDown();
            UUID firstId = futures.get(0).get().paymentAttemptId();
            UUID secondId = futures.get(1).get().paymentAttemptId();
            assertThat(firstId).isEqualTo(secondId);
        } finally {
            pool.shutdown();
        }
        assertThat(repository.findByOrderIdOrderByCreatedAtDesc(orderId)).hasSize(1);
    }

    @Test
    void panOrCvvReferenceIsRejectedBeforePersisting() {
        UUID orderId = UUID.randomUUID();

        assertThatThrownBy(() -> new PaymentRequest(orderId, new BigDecimal("10.00"), "GTQ", "WEB", "CARD",
                "4111111111111111", UUID.randomUUID(), "it-pay-4"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("paymentReference contiene datos sensibles no permitidos.");
        assertThatThrownBy(() -> new PaymentRequest(orderId, new BigDecimal("10.00"), "GTQ", "WEB", "CARD",
                "cvv=123", UUID.randomUUID(), "it-pay-5"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("paymentReference contiene datos sensibles no permitidos.");
        assertThat(repository.findByOrderIdOrderByCreatedAtDesc(orderId)).isEmpty();
    }

    private PaymentResult payAfter(CountDownLatch start, UUID orderId, String idempotencyKey) {
        try {
            start.await();
            return paymentPort.requestPayment(request(orderId, idempotencyKey, UUID.randomUUID()));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
        }
    }

    private PaymentRequest request(UUID orderId, String idempotencyKey, UUID correlationId) {
        return new PaymentRequest(orderId, new BigDecimal("150.00"), "GTQ", "MOBILE", "CARD",
                "tok_test_123", correlationId, idempotencyKey);
    }
}
