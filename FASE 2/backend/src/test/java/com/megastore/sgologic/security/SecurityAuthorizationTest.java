package com.megastore.sgologic.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BB-07: login JWT, 401 sin token y 403 cuando el rol no autoriza el
 * recurso. Usa el contexto completo con PostgreSQL y RabbitMQ reales.
 */
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = "sgo.security.enabled=true")
class SecurityAuthorizationTest {

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
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void loginWithValidCredentialsReturnsTokenWithRoles() throws Exception {
        JsonNode response = login("cliente@megastore.com", "Cliente123");

        assertThat(response.get("accessToken").asText()).isNotBlank();
        assertThat(response.get("tokenType").asText()).isEqualTo("Bearer");
        assertThat(response.get("roles").get(0).asText()).isEqualTo("CLIENT");
    }

    @Test
    void loginWithWrongPasswordReturns401WithCommonError() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "cliente@megastore.com",
                                "password", "incorrecta"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    @Test
    void requestWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/locations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void clientRoleCannotAccessFinanceEndpoint() throws Exception {
        String token = login("cliente@megastore.com", "Cliente123").get("accessToken").asText();

        mockMvc.perform(get("/api/v1/fiscal-documents/orders/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void financeRoleCanAccessFinanceEndpoint() throws Exception {
        String token = login("finanzas@megastore.com", "Finanzas123").get("accessToken").asText();

        mockMvc.perform(get("/api/v1/fiscal-documents/orders/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void staffAccountHasSupportAndLogisticsRoles() throws Exception {
        JsonNode response = login("staff@megastore.com", "Staff123");

        List<String> roles = new ArrayList<>();
        response.get("roles").forEach(role -> roles.add(role.asText()));
        assertThat(roles).contains("SUPPORT", "LOGISTICS");
    }

    @Test
    void clientCanReadTrackingButCannotCreateShipments() throws Exception {
        String clientToken = login("cliente@megastore.com", "Cliente123").get("accessToken").asText();

        mockMvc.perform(get("/api/v1/shipments").param("orderId", UUID.randomUUID().toString())
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/shipments")
                        .header("Authorization", "Bearer " + clientToken)
                        .header("Idempotency-Key", "sec-test-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "orderId", UUID.randomUUID().toString(),
                                "origin", "DISTRIBUTION_CENTER",
                                "destination", "Zona 1"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlySupportCanResolveIncidents() throws Exception {
        String clientToken = login("cliente@megastore.com", "Cliente123").get("accessToken").asText();
        String staffToken = login("staff@megastore.com", "Staff123").get("accessToken").asText();
        String body = objectMapper.writeValueAsString(Map.of("resolution", "APPROVED"));

        mockMvc.perform(patch("/api/v1/incidents/" + UUID.randomUUID() + "/resolution")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        // Con SUPPORT la autorización pasa; la incidencia no existe -> 404.
        mockMvc.perform(patch("/api/v1/incidents/" + UUID.randomUUID() + "/resolution")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void onlyFinanceCanQueryAuditEvidence() throws Exception {
        String clientToken = login("cliente@megastore.com", "Cliente123").get("accessToken").asText();
        String financeToken = login("finanzas@megastore.com", "Finanzas123").get("accessToken").asText();
        String orderId = UUID.randomUUID().toString();

        mockMvc.perform(get("/api/v1/audit/events").param("orderId", orderId)
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());

        // Con FINANCE la autorización pasa; sin evidencia -> 404.
        mockMvc.perform(get("/api/v1/audit/events").param("orderId", orderId)
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("AUDIT_EVIDENCE_NOT_FOUND"));

        // Criterio inválido -> 400 con el formato común.
        mockMvc.perform(get("/api/v1/audit/events")
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_AUDIT_QUERY"));
    }

    @Test
    void paymentsRequireClientOrSupportRole() throws Exception {
        String clientToken = login("cliente@megastore.com", "Cliente123").get("accessToken").asText();
        String financeToken = login("finanzas@megastore.com", "Finanzas123").get("accessToken").asText();
        String body = objectMapper.writeValueAsString(Map.of(
                "orderId", UUID.randomUUID().toString(),
                "amount", 150.00,
                "currency", "GTQ",
                "channel", "MOBILE",
                "method", "CARD",
                "paymentReference", "tok_test_123"));

        mockMvc.perform(post("/api/v1/payments/attempts")
                        .header("Authorization", "Bearer " + financeToken)
                        .header("Idempotency-Key", "sec-pay-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/payments/attempts")
                        .header("Authorization", "Bearer " + clientToken)
                        .header("Idempotency-Key", "sec-pay-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/payments/attempts")
                        .param("orderId", UUID.randomUUID().toString())
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk());
    }

    @Test
    void roleMatrixRestrictsEndpoints() throws Exception {
        String client = login("cliente@megastore.com", "Cliente123").get("accessToken").asText();
        String logistics = login("logistica@megastore.com", "Logistica123").get("accessToken").asText();
        String finance = login("finanzas@megastore.com", "Finanzas123").get("accessToken").asText();
        String staff = login("staff@megastore.com", "Staff123").get("accessToken").asText();
        String orderId = UUID.randomUUID().toString();

        // Sin token: 401 en los grupos protegidos.
        mockMvc.perform(get("/api/v1/payments/attempts").param("orderId", orderId))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/fiscal-documents/orders/" + orderId))
                .andExpect(status().isUnauthorized());

        // CLIENT no puede ingerir proyecciones, emitir notificaciones ni ver fiscal.
        mockMvc.perform(post("/api/v1/query/order-summary")
                        .header("Authorization", "Bearer " + client)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"" + orderId + "\",\"source\":\"ORDER\",\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/notifications")
                        .header("Authorization", "Bearer " + client)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventType\":\"ORDER_CONFIRMED\",\"channel\":\"EMAIL\","
                                + "\"recipient\":\"c@example.com\",\"sourceAggregateId\":\"" + orderId + "\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/fiscal-documents/orders/" + orderId)
                        .header("Authorization", "Bearer " + client))
                .andExpect(status().isForbidden());

        // LOGISTICS no puede ver pagos ni auditoría.
        mockMvc.perform(get("/api/v1/payments/attempts").param("orderId", orderId)
                        .header("Authorization", "Bearer " + logistics))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/audit/events").param("orderId", orderId)
                        .header("Authorization", "Bearer " + logistics))
                .andExpect(status().isForbidden());

        // FINANCE no puede operar despachos ni pagos.
        mockMvc.perform(get("/api/v1/shipments").param("orderId", orderId)
                        .header("Authorization", "Bearer " + finance))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/payments/attempts").param("orderId", orderId)
                        .header("Authorization", "Bearer " + finance))
                .andExpect(status().isForbidden());

        // SUPPORT (staff) no puede ver fiscal.
        mockMvc.perform(get("/api/v1/fiscal-documents/orders/" + orderId)
                        .header("Authorization", "Bearer " + staff))
                .andExpect(status().isForbidden());
    }

    @Test
    void clientRoleCanReadInventoryAndPublicHealthStaysOpen() throws Exception {
        String token = login("cliente@megastore.com", "Cliente123").get("accessToken").asText();

        mockMvc.perform(get("/api/v1/inventory/locations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    private JsonNode login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "password", password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body);
    }
}
