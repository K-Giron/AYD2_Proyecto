package com.megastore.sgologic.payments;

import com.megastore.sgologic.payments.api.PaymentMethod;

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
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PAY-03 contra PostgreSQL y RabbitMQ reales: un intento indeterminado se
 * concilia reutilizando su Idempotency-Key, publica {@code PaymentApproved}
 * y una segunda pasada no genera un segundo cobro ni un segundo evento.
 */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PaymentReconciliationIntegrationTest {

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
    PaymentReconciliationService reconciliationService;

    @Autowired
    PaymentReconciliationRepository reconciliationRepository;

    @Autowired
    PaymentAttemptRepository attemptRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    Clock clock;

    @Test
    void recoversStalePendingAttempt() {
        Instant now = clock.instant();
        PaymentAttempt pending = PaymentAttempt.create(UUID.randomUUID(), UUID.randomUUID(),
                PaymentChannel.MOBILE, PaymentMethod.CARD, new BigDecimal("150.00"), "GTQ",
                UUID.randomUUID(), "it-stale-" + UUID.randomUUID(), "tok_test_123", now.minusSeconds(300));
        attemptRepository.save(pending);

        reconciliationService.reconcileDue();

        PaymentAttempt recovered = attemptRepository.findById(pending.getId()).orElseThrow();
        assertThat(recovered.getStatus()).isEqualTo(PaymentAttemptStatus.INDETERMINATE);
        assertThat(recovered.getFailureCode()).isEqualTo("STALE_PENDING");
        assertThat(reconciliationRepository.findByPaymentAttemptIdAndStatus(
                pending.getId(), PaymentReconciliationStatus.PENDING)).isPresent();
    }

    @Test
    void reconcilesIndeterminateAttemptWithoutSecondCharge() {
        Instant now = clock.instant();
        PaymentAttempt attempt = PaymentAttempt.create(UUID.randomUUID(), UUID.randomUUID(),
                PaymentChannel.MOBILE, PaymentMethod.CARD, new BigDecimal("150.00"), "GTQ",
                UUID.randomUUID(), "it-rec-" + UUID.randomUUID(), "tok_test_123", now.minusSeconds(120));
        attempt.markIndeterminate("PAYKLIK_TIMEOUT", "sin respuesta", now.minusSeconds(120));
        attemptRepository.save(attempt);
        PaymentReconciliation reconciliation = PaymentReconciliation.schedule(
                attempt.getId(), now.minusSeconds(5), now.minusSeconds(120));
        reconciliationRepository.save(reconciliation);

        reconciliationService.reconcileDue();

        assertThat(attemptRepository.findById(attempt.getId()).orElseThrow().getStatus())
                .isEqualTo(PaymentAttemptStatus.APPROVED);
        PaymentReconciliation stored = reconciliationRepository.findById(reconciliation.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(PaymentReconciliationStatus.RESOLVED);
        assertThat(stored.getResult()).isEqualTo("APPROVED");

        Integer approvedEvents = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM outbox_event WHERE aggregate_id = ? AND event_type = 'PaymentApproved'",
                Integer.class, attempt.getId());
        Integer auditRows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_events WHERE aggregate_id = ? AND action = 'PAYMENT_APPROVED'",
                Integer.class, attempt.getId());
        assertThat(approvedEvents).isEqualTo(1);
        assertThat(auditRows).isEqualTo(1);

        // Segunda pasada: el intento ya está resuelto, no hay nuevo cobro ni evento.
        reconciliationService.reconcileDue();

        assertThat(attemptRepository.findById(attempt.getId()).orElseThrow().getStatus())
                .isEqualTo(PaymentAttemptStatus.APPROVED);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM outbox_event WHERE aggregate_id = ? AND event_type = 'PaymentApproved'",
                Integer.class, attempt.getId())).isEqualTo(1);
        assertThat(reconciliationRepository.findById(reconciliation.getId()).orElseThrow().getAttempts())
                .isZero();
    }
}
