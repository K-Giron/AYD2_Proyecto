package com.megastore.sgologic.payments;

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
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PAY-02 contra PostgreSQL y RabbitMQ reales con PayKlik en modo TIMEOUT:
 * la orden y el intento se conservan, el intento queda INDETERMINATE, deja
 * evidencia auditable y publica {@code PaymentIndeterminate}; tras la racha
 * de fallas el circuit breaker abre y deja de llamar al proveedor.
 */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
        "sgo.simulators.payklik.mode=TIMEOUT",
        "sgo.payments.payklik.timeout-ms=3000",
        "sgo.payments.payklik.minimum-number-of-calls=2",
        "sgo.payments.payklik.sliding-window-size=2",
        "sgo.payments.payklik.wait-duration-in-open-state=60s"
})
class PaymentIndeterminateIntegrationTest {

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
    PaymentAttemptRepository repository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void timeoutsKeepOrderAndAttemptAndOpenTheCircuit() {
        UUID firstOrder = UUID.randomUUID();
        PaymentResult first = paymentPort.requestPayment(request(firstOrder, "it-ind-1"));

        assertThat(first.status()).isEqualTo("INDETERMINATE");
        PaymentAttempt firstAttempt = repository.findById(first.paymentAttemptId()).orElseThrow();
        assertThat(firstAttempt.getOrderId()).isEqualTo(firstOrder);
        assertThat(firstAttempt.getStatus()).isEqualTo(PaymentAttemptStatus.INDETERMINATE);
        assertThat(firstAttempt.getFailureCode()).isEqualTo("PAYKLIK_TIMEOUT");

        Integer auditRows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_events WHERE aggregate_id = ? AND action = 'PAYMENT_INDETERMINATE'",
                Integer.class, firstAttempt.getId());
        assertThat(auditRows).isEqualTo(1);

        String eventType = jdbcTemplate.queryForObject(
                "SELECT event_type FROM outbox_event WHERE aggregate_id = ?", String.class, firstAttempt.getId());
        String payload = jdbcTemplate.queryForObject(
                "SELECT payload FROM outbox_event WHERE aggregate_id = ?", String.class, firstAttempt.getId());
        assertThat(eventType).isEqualTo("PaymentIndeterminate");
        assertThat(payload).contains(firstOrder.toString()).doesNotContainPattern("\\d{13,}");

        // PAY-03: la conciliación queda programada dentro de los 60 s.
        Map<String, Object> reconciliation = jdbcTemplate.queryForMap(
                "SELECT status, EXTRACT(EPOCH FROM (scheduled_at - created_at)) AS delay_seconds "
                        + "FROM payment_reconciliations WHERE payment_attempt_id = ?", firstAttempt.getId());
        assertThat(reconciliation.get("status")).isEqualTo("PENDING");
        assertThat(((Number) reconciliation.get("delay_seconds")).longValue()).isBetween(0L, 60L);

        PaymentResult second = paymentPort.requestPayment(request(UUID.randomUUID(), "it-ind-2"));
        assertThat(second.status()).isEqualTo("INDETERMINATE");
        assertThat(repository.findById(second.paymentAttemptId()).orElseThrow().getFailureCode())
                .isEqualTo("PAYKLIK_TIMEOUT");

        PaymentResult third = paymentPort.requestPayment(request(UUID.randomUUID(), "it-ind-3"));
        assertThat(third.status()).isEqualTo("INDETERMINATE");
        assertThat(repository.findById(third.paymentAttemptId()).orElseThrow().getFailureCode())
                .isEqualTo("PAYKLIK_CIRCUIT_OPEN");

        assertThat(repository.count()).isEqualTo(3);
    }

    private PaymentRequest request(UUID orderId, String idempotencyKey) {
        return new PaymentRequest(orderId, new BigDecimal("150.00"), "GTQ", "MOBILE", "CARD",
                "tok_test_123", UUID.randomUUID(), idempotencyKey);
    }
}
