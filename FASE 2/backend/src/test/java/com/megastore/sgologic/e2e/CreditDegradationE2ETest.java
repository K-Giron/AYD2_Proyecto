package com.megastore.sgologic.e2e;

import com.megastore.sgologic.credit.api.CreditEvaluationRequest;
import com.megastore.sgologic.credit.api.CreditEvaluationResult;
import com.megastore.sgologic.credit.api.CreditOperationResult;
import com.megastore.sgologic.credit.api.CreditPort;
import com.megastore.sgologic.credit.api.CreditRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * E2E-03 (EDS3-06): cuando la evaluación de crédito no está disponible (falla
 * o tarda más que el tiempo límite), el checkout sigue ofreciendo PayKlik, la
 * Saga completa la compra y no se crea ninguna obligación de crédito.
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
        "sgo.simulators.notifications.mode=SUCCESS"
})
class CreditDegradationE2ETest {

    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111102");
    private static final UUID DEMO_CUSTOMER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final Duration AWAIT_TIMEOUT = Duration.ofSeconds(15);

    enum EvaluationFailure { THROW, SLOW }

    private static final AtomicReference<EvaluationFailure> FAILURE =
            new AtomicReference<>(EvaluationFailure.THROW);

    @TestConfiguration
    static class UnavailableCreditConfiguration {

        @Bean
        @Primary
        CreditPort unavailableCreditPort() {
            return new CreditPort() {
                @Override
                public CreditEvaluationResult evaluate(CreditEvaluationRequest request) {
                    if (FAILURE.get() == EvaluationFailure.SLOW) {
                        try {
                            Thread.sleep(4000);
                        } catch (InterruptedException interrupted) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    throw new IllegalStateException("Evaluación de crédito no disponible (E2E-03).");
                }

                @Override
                public CreditOperationResult requestCredit(CreditRequest request) {
                    throw new IllegalStateException("No debe solicitarse crédito en E2E-03.");
                }

                @Override
                public Optional<CreditOperationResult> latestForOrder(UUID orderId) {
                    return Optional.empty();
                }
            };
        }
    }

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17.11-alpine3.23"))
            .withDatabaseName("sgo_credit_degradation_e2e")
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

    @BeforeEach
    void resetFailureMode() {
        FAILURE.set(EvaluationFailure.THROW);
    }

    @Test
    void unavailableCreditEvaluationStillOffersPayKlikAndCompletesTheCheckout() throws Exception {
        String client = login("cliente@megastore.com", "Cliente123");
        String cartId = openCartWithProduct(client);

        JsonNode options = getJson("/api/v1/carts/" + cartId + "/payment-options", client);

        assertThat(options.get("options").size()).isEqualTo(1);
        assertThat(options.get("options").get(0).get("route").asText()).isEqualTo("PAYKLIK");

        JsonNode order = checkout(client, cartId, key("checkout-degraded"));
        JsonNode saga = awaitSaga(client, order.get("id").asText(), key("saga-degraded"), "Zona 10, Guatemala");

        assertThat(saga.get("sagaStatus").asText()).isEqualTo("COMPLETED");
        assertThat(getJson("/api/v1/orders/" + order.get("id").asText(), client)
                .get("status").asText()).isEqualTo("PREPARING");
        UUID orderId = UUID.fromString(order.get("id").asText());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM credit_operations WHERE order_id = ?", Integer.class, orderId))
                .isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM payment_attempts WHERE order_id = ?", Integer.class, orderId))
                .isEqualTo(1);
    }

    @Test
    void slowCreditEvaluationTimesOutWithinBoundAndKeepsPayKlik() throws Exception {
        FAILURE.set(EvaluationFailure.SLOW);
        String client = login("cliente@megastore.com", "Cliente123");
        String cartId = openCartWithProduct(client);

        long started = System.nanoTime();
        JsonNode options = getJson("/api/v1/carts/" + cartId + "/payment-options", client);
        long elapsedMillis = (System.nanoTime() - started) / 1_000_000;

        assertThat(options.get("options").size()).isEqualTo(1);
        assertThat(options.get("options").get(0).get("route").asText()).isEqualTo("PAYKLIK");
        assertThat(elapsedMillis).isLessThan(6000);
    }

    private String openCartWithProduct(String token) throws Exception {
        JsonNode cart = postJson("/api/v1/carts", token, Map.of(
                "customerId", DEMO_CUSTOMER_ID.toString(), "channel", "WEB"),
                Map.of("Idempotency-Key", key("cart-degraded")));
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

    private JsonNode awaitSaga(String token, String orderId, String sagaKey, String destination) throws Exception {
        long deadline = System.nanoTime() + AWAIT_TIMEOUT.toNanos();
        JsonNode result;
        do {
            result = postJson("/api/v1/orders/" + orderId + "/saga", token, Map.of(
                    "paymentMethod", "CARD", "currency", "GTQ", "paymentReference", "tok_e2e_4242",
                    "taxIdentifier", "CF-E2E", "customerName", "Cliente E2E", "destination", destination),
                    Map.of("Idempotency-Key", sagaKey, "X-Actor", "customer:e2e"));
            if (!"WAITING_FISCAL".equals(result.get("sagaStatus").asText())) return result;
            Thread.sleep(150);
        } while (System.nanoTime() < deadline);
        throw new AssertionError("La Saga no salió de WAITING_FISCAL: " + result);
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
}
