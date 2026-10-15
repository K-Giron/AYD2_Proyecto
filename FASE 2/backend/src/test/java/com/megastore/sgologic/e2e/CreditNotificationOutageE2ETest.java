package com.megastore.sgologic.e2e;

import com.megastore.sgologic.notifications.NotificationChannel;
import com.megastore.sgologic.notifications.NotificationEventType;
import com.megastore.sgologic.notifications.NotificationProviderPort;
import com.megastore.sgologic.notifications.NotificationProviderUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * E2E-05 (EDS3-06): la caída del proveedor de notificaciones no revierte la
 * orden ni el crédito; el mensaje de resultado falla una vez, se reintenta con
 * el backoff del trabajador y termina entregado.
 */
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = {
        "sgo.security.enabled=true",
        "sgo.simulators.payklik.mode=SUCCESS",
        "sgo.simulators.sat.mode=SUCCESS",
        "sgo.simulators.logiexpress.mode=SUCCESS",
        "sgo.simulators.notifications.mode=SUCCESS",
        "sgo.messaging.consumers.enabled=true",
        "sgo.messaging.outbox.enabled=true",
        "sgo.outbox.poll-interval-ms=500"
})
class CreditNotificationOutageE2ETest {

    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111102");
    private static final UUID DEMO_CUSTOMER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final Duration AWAIT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration NOTIFICATION_TIMEOUT = Duration.ofSeconds(30);

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17.11-alpine3.23"))
            .withDatabaseName("sgo_credit_outage_e2e")
            .withUsername("sgo_e2e")
            .withPassword("e2e_only_password");

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

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbcTemplate;

    @MockitoBean(name = "channelRoutingNotificationProvider")
    NotificationProviderPort provider;

    @Test
    void creditNotificationFailsOnceButOrderStaysConfirmedAndMessageRetries() throws Exception {
        AtomicInteger creditAttempts = new AtomicInteger();
        doAnswer(invocation -> {
            NotificationEventType eventType = invocation.getArgument(2);
            if (eventType == NotificationEventType.CREDIT_GRANTED
                    && creditAttempts.incrementAndGet() == 1) {
                throw new NotificationProviderUnavailableException(
                        "Proveedor caído una vez (E2E-05).");
            }
            return null;
        }).when(provider).send(any(), any(), any(), any());

        String client = login("cliente@megastore.com", "Cliente123");
        String cartId = openCartWithProduct(client);
        JsonNode order = checkout(client, cartId, key("checkout-outage"));
        String orderId = order.get("id").asText();
        JsonNode saga = awaitCreditSaga(client, orderId, key("saga-outage"));

        assertThat(saga.get("sagaStatus").asText()).isEqualTo("COMPLETED");
        assertThat(getJson("/api/v1/orders/" + orderId, client).get("status").asText())
                .isEqualTo("PREPARING");

        UUID aggregateId = UUID.fromString(orderId);
        NotificationRow notification = awaitSentCreditNotification(aggregateId);

        assertThat(notification.attempts()).isEqualTo(2);
        assertThat(notification.sentAt()).isNotNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM credit_operations WHERE order_id = ?", String.class, aggregateId))
                .isEqualTo("GRANTED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM credit_operations WHERE order_id = ?", Integer.class, aggregateId))
                .isEqualTo(1);
        verify(provider, times(2)).send(eq(NotificationChannel.EMAIL), eq("cliente@megastore.test"),
                eq(NotificationEventType.CREDIT_GRANTED), eq(aggregateId));
    }

    private NotificationRow awaitSentCreditNotification(UUID orderId) throws InterruptedException {
        Instant deadline = Instant.now().plus(NOTIFICATION_TIMEOUT);
        List<NotificationRow> rows = List.of();
        do {
            rows = jdbcTemplate.query("SELECT event_type, status, attempts, sent_at "
                            + "FROM notification_requests WHERE source_aggregate_id = ? "
                            + "AND event_type = 'CREDIT_GRANTED'",
                    (resultSet, rowNumber) -> new NotificationRow(
                            resultSet.getString("event_type"),
                            resultSet.getString("status"),
                            resultSet.getInt("attempts"),
                            resultSet.getTimestamp("sent_at") == null ? null
                                    : resultSet.getTimestamp("sent_at").toInstant()),
                    orderId);
            if (!rows.isEmpty() && "SENT".equals(rows.get(0).status())) {
                return rows.get(0);
            }
            Thread.sleep(250);
        } while (Instant.now().isBefore(deadline));
        throw new AssertionError("La notificación CREDIT_GRANTED de " + orderId
                + " no llegó a SENT tras el reintento: " + rows);
    }

    private String openCartWithProduct(String token) throws Exception {
        JsonNode cart = postJson("/api/v1/carts", token, Map.of(
                "customerId", DEMO_CUSTOMER_ID.toString(), "channel", "WEB"),
                Map.of("Idempotency-Key", key("cart-outage")));
        String cartId = cart.get("id").asText();
        mvc.perform(put("/api/v1/carts/" + cartId + "/items/" + PRODUCT_ID)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "productName", "Pantalón de lona negro",
                                "unitPrice", new BigDecimal("399.00"), "quantity", 1))))
                .andExpect(status().isOk());
        return cartId;
    }

    private JsonNode awaitCreditSaga(String token, String orderId, String sagaKey) throws Exception {
        long deadline = System.nanoTime() + AWAIT_TIMEOUT.toNanos();
        JsonNode result;
        do {
            result = postJson("/api/v1/orders/" + orderId + "/saga", token, Map.of(
                    "paymentRoute", "OWN_CREDIT", "currency", "GTQ",
                    "taxIdentifier", "CF-CREDIT", "customerName", "Cliente Crédito",
                    "destination", "Zona 10, Guatemala"),
                    Map.of("Idempotency-Key", sagaKey, "X-Actor", "customer:credit-e2e"));
            if (!"WAITING_FISCAL".equals(result.get("sagaStatus").asText())) return result;
            Thread.sleep(150);
        } while (System.nanoTime() < deadline);
        throw new AssertionError("La Saga de crédito no salió de WAITING_FISCAL: " + result);
    }

    private JsonNode checkout(String token, String cartId, String key) throws Exception {
        return postJson("/api/v1/carts/" + cartId + "/checkout", token, null,
                Map.of("Idempotency-Key", key, "X-Actor", "customer:e2e"));
    }

    private String login(String username, String password) throws Exception {
        return postJson("/api/v1/auth/login", null, Map.of("username", username, "password", password), Map.of())
                .get("accessToken").asText();
    }

    private JsonNode getJson(String path, String token) throws Exception {
        String content = mvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(content);
    }

    private JsonNode postJson(String path, String token, Object body, Map<String, String> headers)
            throws Exception {
        var request = post(path).contentType(MediaType.APPLICATION_JSON);
        if (token != null) request.header("Authorization", "Bearer " + token);
        headers.forEach(request::header);
        if (body != null) request.content(mapper.writeValueAsString(body));
        String content = mvc.perform(request).andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(content);
    }

    private static String key(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private record NotificationRow(String eventType, String status, int attempts, Instant sentAt) {
    }
}
