package com.megastore.sgologic.notifications;

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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * IT-05 (EDS3-05): el evento de decisión publica por Outbox, viaja por RabbitMQ,
 * lo consume el Inbox de Notifications una sola vez y, si el proveedor falla,
 * reintenta con el backoff 2/8/32 s y termina en la cola de fallidos, sin
 * revertir la concesión de crédito.
 */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
@TestPropertySource(properties = {
        "sgo.messaging.consumers.enabled=true",
        "sgo.messaging.outbox.enabled=true",
        "sgo.outbox.poll-interval-ms=500",
        "sgo.simulators.notifications.mode=SUCCESS"
})
class CreditNotificationIntegrationTest {

    private static final UUID DEMO_CUSTOMER_ID =
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final Duration AWAIT = Duration.ofSeconds(25);
    private static final Duration RETRY_AWAIT = Duration.ofSeconds(40);
    private static final Duration DLQ_AWAIT = Duration.ofSeconds(90);

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17.11-alpine3.23"))
            .withDatabaseName("sgo_credit_notifications_test")
            .withUsername("sgo_test")
            .withPassword("test_only_password");

    @Container
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:4.3.5-management-alpine"));

    @DynamicPropertySource
    static void infrastructure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);
    }

    @Autowired CreditPort credit;
    @Autowired JdbcTemplate jdbc;

    @MockitoBean(name = "channelRoutingNotificationProvider")
    NotificationProviderPort provider;

    @BeforeEach
    void resetLedger() {
        jdbc.update("DELETE FROM notification_dead_letters");
        jdbc.update("DELETE FROM notification_requests");
        jdbc.update("DELETE FROM credit_operations");
        jdbc.update("DELETE FROM credit_decisions");
        jdbc.update("UPDATE credit_customer_profiles SET available_credit = 2500.00, version = 0 "
                + "WHERE customer_id = ?", DEMO_CUSTOMER_ID);
    }

    @Test
    void grantedCreditCreatesExactlyOneSentNotification() throws Exception {
        UUID orderId = UUID.randomUUID();
        CreditOperationResult granted = credit.requestCredit(
                request(orderId, DEMO_CUSTOMER_ID, "399.00"));
        assertThat(granted.status()).isEqualTo(CreditOperationStatus.GRANTED);

        NotificationRow notification = awaitNotification(orderId, "SENT", AWAIT);

        assertThat(notification.eventType()).isEqualTo("CREDIT_GRANTED");
        assertThat(notification.channel()).isEqualTo("EMAIL");
        assertThat(notification.recipient()).isEqualTo("cliente@megastore.test");
        assertThat(notification.attempts()).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM notification_requests WHERE source_aggregate_id = ?",
                Integer.class, orderId)).isEqualTo(1);
        verify(provider).send(NotificationChannel.EMAIL, "cliente@megastore.test",
                NotificationEventType.CREDIT_GRANTED, orderId);
    }

    @Test
    void rejectedCreditNotifiesTheRejectionWithoutCreatingDebt() throws Exception {
        UUID orderId = UUID.randomUUID();
        CreditOperationResult rejected = credit.requestCredit(
                request(orderId, DEMO_CUSTOMER_ID, "99999.00"));
        assertThat(rejected.status()).isEqualTo(CreditOperationStatus.REJECTED);

        NotificationRow notification = awaitNotification(orderId, "SENT", AWAIT);

        assertThat(notification.eventType()).isEqualTo("CREDIT_REJECTED");
        assertThat(jdbc.queryForObject("SELECT due_date FROM credit_operations WHERE order_id = ?",
                Object.class, orderId)).isNull();
        assertThat(jdbc.queryForObject(
                "SELECT available_credit FROM credit_customer_profiles WHERE customer_id = ?",
                BigDecimal.class, DEMO_CUSTOMER_ID)).isEqualByComparingTo("2500.00");
        verify(provider).send(NotificationChannel.EMAIL, "cliente@megastore.test",
                NotificationEventType.CREDIT_REJECTED, orderId);
    }

    @Test
    void outageExhaustsRetriesAndMovesToDeadLetterWithoutReversingTheCredit() throws Exception {
        doThrow(new NotificationProviderUnavailableException("caído"))
                .when(provider).send(any(), any(), any(), any());
        UUID orderId = UUID.randomUUID();
        credit.requestCredit(request(orderId, DEMO_CUSTOMER_ID, "399.00"));

        NotificationRow notification = awaitNotification(orderId, "FAILED", DLQ_AWAIT);

        assertThat(notification.attempts()).isEqualTo(4);
        assertThat(notification.lastError()).isNotBlank();
        UUID notificationId = notification.id();
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM notification_dead_letters WHERE notification_id = ?",
                Integer.class, notificationId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM credit_operations WHERE order_id = ?",
                String.class, orderId)).isEqualTo("GRANTED");
        assertThat(jdbc.queryForObject(
                "SELECT available_credit FROM credit_customer_profiles WHERE customer_id = ?",
                BigDecimal.class, DEMO_CUSTOMER_ID)).isEqualByComparingTo("2101.00");
        verify(provider, times(4)).send(eq(NotificationChannel.EMAIL), eq("cliente@megastore.test"),
                eq(NotificationEventType.CREDIT_GRANTED), eq(orderId));
    }

    @Test
    void providerRecoversOnFirstRetryAndDeliversTheMessage() throws Exception {
        doThrow(new NotificationProviderUnavailableException("caído"))
                .doNothing()
                .when(provider).send(any(), any(), any(), any());
        UUID orderId = UUID.randomUUID();
        credit.requestCredit(request(orderId, DEMO_CUSTOMER_ID, "399.00"));

        NotificationRow notification = awaitNotification(orderId, "SENT", RETRY_AWAIT);

        assertThat(notification.attempts()).isEqualTo(2);
        assertThat(notification.sentAt()).isNotNull();
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM notification_dead_letters", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT status FROM credit_operations WHERE order_id = ?",
                String.class, orderId)).isEqualTo("GRANTED");
        verify(provider, times(2)).send(NotificationChannel.EMAIL, "cliente@megastore.test",
                NotificationEventType.CREDIT_GRANTED, orderId);
    }

    private CreditRequest request(UUID orderId, UUID customerId, String amount) {
        return new CreditRequest(orderId, customerId, new BigDecimal(amount), "GTQ",
                UUID.randomUUID(), "credit-notify-" + UUID.randomUUID());
    }

    private NotificationRow awaitNotification(UUID orderId, String expectedStatus, Duration timeout)
            throws InterruptedException {
        Instant deadline = Instant.now().plus(timeout);
        List<NotificationRow> rows = List.of();
        do {
            rows = jdbc.query("SELECT id, event_type, status, channel, recipient, attempts, last_error, sent_at "
                            + "FROM notification_requests WHERE source_aggregate_id = ?",
                    (resultSet, rowNumber) -> new NotificationRow(
                            resultSet.getObject("id", UUID.class),
                            resultSet.getString("event_type"),
                            resultSet.getString("status"),
                            resultSet.getString("channel"),
                            resultSet.getString("recipient"),
                            resultSet.getInt("attempts"),
                            resultSet.getString("last_error"),
                            resultSet.getTimestamp("sent_at") == null ? null
                                    : resultSet.getTimestamp("sent_at").toInstant()),
                    orderId);
            if (!rows.isEmpty() && expectedStatus.equals(rows.get(0).status())) {
                return rows.get(0);
            }
            Thread.sleep(250);
        } while (Instant.now().isBefore(deadline));
        throw new AssertionError("La notificación de " + orderId + " no llegó a " + expectedStatus
                + " en " + timeout + ": " + rows);
    }

    private record NotificationRow(UUID id, String eventType, String status, String channel,
                                   String recipient, int attempts, String lastError, Instant sentAt) {
    }
}
