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

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    void logisticsRoleCanAccessShipmentsAndClientCannot() throws Exception {
        String logisticsToken = login("logistica@megastore.com", "Logistica123").get("accessToken").asText();
        String clientToken = login("cliente@megastore.com", "Cliente123").get("accessToken").asText();

        mockMvc.perform(get("/api/v1/shipments").param("orderId", UUID.randomUUID().toString())
                        .header("Authorization", "Bearer " + logisticsToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/shipments").param("orderId", UUID.randomUUID().toString())
                        .header("Authorization", "Bearer " + clientToken))
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
