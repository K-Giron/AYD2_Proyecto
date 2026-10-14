package com.megastore.sgologic.orders;

import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * IT-02 (EDS3-05): el checkout consulta la elegibilidad por HTTP contra
 * PostgreSQL real y expone las opciones correctas — PayKlik primero, crédito
 * solo para el perfil elegible — y la propiedad del carrito se aplica.
 */
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = "sgo.security.enabled=true")
class PaymentOptionsIntegrationTest {

    private static final UUID DEMO_CUSTOMER_ID =
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID PRODUCT_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111102");

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17.11-alpine3.23"))
            .withDatabaseName("sgo_payment_options_test")
            .withUsername("sgo_test")
            .withPassword("test_only_password");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void restoreEligibleProfile() {
        jdbc.update("INSERT INTO credit_customer_profiles (customer_id, completed_purchases, "
                        + "on_time_payment_rate, return_rate, critical_incidents, has_overdue_credit, "
                        + "available_credit, observed_at, version) "
                        + "VALUES (?, 8, 1.0000, 0.1250, 0, FALSE, 2500.00, now(), 0) "
                        + "ON CONFLICT (customer_id) DO UPDATE SET completed_purchases = 8, "
                        + "on_time_payment_rate = 1.0000, return_rate = 0.1250, critical_incidents = 0, "
                        + "has_overdue_credit = FALSE, available_credit = 2500.00, observed_at = now(), "
                        + "version = 0", DEMO_CUSTOMER_ID);
    }

    @Test
    void eligibleCustomerSeesPayKlikFirstAndOwnCreditWithDueDatePreview() throws Exception {
        String client = login("cliente@megastore.com", "Cliente123");
        String cartId = openCartWithProduct(client);

        JsonNode options = getJson("/api/v1/carts/" + cartId + "/payment-options", client);

        assertThat(options.get("ruleVersion").asText()).isEqualTo("credit-rules-v1");
        assertThat(options.get("total").decimalValue()).isEqualByComparingTo("399.00");
        assertThat(options.get("options").size()).isEqualTo(2);
        assertThat(options.get("options").get(0).get("route").asText()).isEqualTo("PAYKLIK");
        assertThat(options.get("options").get(1).get("route").asText()).isEqualTo("OWN_CREDIT");
        assertThat(options.get("options").get(1).get("enabled").asBoolean()).isTrue();
        assertThat(options.get("options").get(1).get("approvedLimit").decimalValue())
                .isEqualByComparingTo("2500.00");
        assertThat(LocalDate.parse(options.get("options").get(1).get("dueDatePreview").asText()))
                .isEqualTo(LocalDate.now(ZoneOffset.UTC).plusDays(30));

        // La evaluación quedó trazada en la base real, no en un doble de prueba.
        assertThat(jdbc.queryForObject(
                "SELECT eligible FROM credit_decisions WHERE customer_id = ? "
                        + "ORDER BY evaluated_at DESC LIMIT 1", Boolean.class, DEMO_CUSTOMER_ID))
                .isTrue();
    }

    @Test
    void customerWithoutHistorySeesOnlyPayKlik() throws Exception {
        String client = login("cliente@megastore.com", "Cliente123");
        String cartId = openCartWithProduct(client);
        jdbc.update("DELETE FROM credit_customer_profiles WHERE customer_id = ?", DEMO_CUSTOMER_ID);

        JsonNode options = getJson("/api/v1/carts/" + cartId + "/payment-options", client);

        assertThat(options.get("options").size()).isEqualTo(1);
        assertThat(options.get("options").get(0).get("route").asText()).isEqualTo("PAYKLIK");
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM credit_decisions WHERE customer_id = ? AND eligible = FALSE",
                Integer.class, DEMO_CUSTOMER_ID)).isGreaterThanOrEqualTo(1);
    }

    @Test
    void anotherCustomersCartIsDeniedAndAnonymousAccessIsUnauthorized() throws Exception {
        UUID foreignCartId = UUID.randomUUID();
        jdbc.update("INSERT INTO carts (id, customer_id, status, origin_channel, last_channel, "
                        + "idempotency_key, correlation_id, created_at, updated_at, version) "
                        + "VALUES (?, ?, 'ACTIVE', 'WEB', 'WEB', ?, ?, now(), now(), 0)",
                foreignCartId, UUID.randomUUID(), "foreign-cart-" + foreignCartId, UUID.randomUUID());

        mockMvc.perform(get("/api/v1/carts/" + foreignCartId + "/payment-options")
                        .header("Authorization", "Bearer " + login("cliente@megastore.com", "Cliente123")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mockMvc.perform(get("/api/v1/carts/" + foreignCartId + "/payment-options"))
                .andExpect(status().isUnauthorized());
    }

    private String openCartWithProduct(String token) throws Exception {
        String cart = mockMvc.perform(post("/api/v1/carts")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "options-cart-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "customerId", DEMO_CUSTOMER_ID.toString(), "channel", "WEB"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String cartId = objectMapper.readTree(cart).get("id").asText();
        mockMvc.perform(put("/api/v1/carts/" + cartId + "/items/" + PRODUCT_ID)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productName", "Pantalón de lona negro",
                                "unitPrice", new BigDecimal("399.00"), "quantity", 1))))
                .andExpect(status().isOk());
        return cartId;
    }

    private JsonNode getJson(String path, String token) throws Exception {
        String content = mockMvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(content);
    }

    private String login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }
}
