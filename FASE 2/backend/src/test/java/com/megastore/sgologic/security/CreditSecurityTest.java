package com.megastore.sgologic.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Matriz 401/403 del módulo de crédito (EDS3-03): solo Finanzas puede cargar
 * historial del ERP y registrar el pago de una obligación.
 */
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = "sgo.security.enabled=true")
class CreditSecurityTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17.11-alpine3.23"))
            .withDatabaseName("sgo_credit_security_test")
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

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void historyWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/credit/history").contentType(MediaType.APPLICATION_JSON)
                        .content(validBatch()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void clientCannotLoadCreditHistory() throws Exception {
        mockMvc.perform(post("/api/v1/credit/history").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token("cliente@megastore.com", "Cliente123"))
                        .content(validBatch()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void supportAndLogisticsCannotLoadCreditHistory() throws Exception {
        mockMvc.perform(post("/api/v1/credit/history").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token("staff@megastore.com", "Staff123"))
                        .content(validBatch()))
                .andExpect(status().isForbidden());
    }

    @Test
    void financeCanLoadCreditHistory() throws Exception {
        mockMvc.perform(post("/api/v1/credit/history").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token("finanzas@megastore.com", "Finanzas123"))
                        .content(validBatch()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(1));
    }

    @Test
    void financeGetsA400ForAnInvalidBatchWithoutLeakingInternals() throws Exception {
        String wrongVersion = objectMapper.writeValueAsString(Map.of(
                "contractVersion", "credit-history-v9",
                "correlationId", UUID.randomUUID().toString(),
                "entries", List.of(purchase("sec-bad-" + UUID.randomUUID()))));

        mockMvc.perform(post("/api/v1/credit/history").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token("finanzas@megastore.com", "Finanzas123"))
                        .content(wrongVersion))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CREDIT_REQUEST"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    @Test
    void settleRequiresFinance() throws Exception {
        String url = "/api/v1/credit/operations/" + UUID.randomUUID() + "/settle";

        mockMvc.perform(post(url)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(url).header("Authorization",
                        "Bearer " + token("cliente@megastore.com", "Cliente123")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(url).header("Authorization",
                        "Bearer " + token("staff@megastore.com", "Staff123")))
                .andExpect(status().isForbidden());
    }

    @Test
    void financeSettlingAnUnknownOperationGetsA404() throws Exception {
        mockMvc.perform(post("/api/v1/credit/operations/" + UUID.randomUUID() + "/settle")
                        .header("Authorization", "Bearer " + token("finanzas@megastore.com", "Finanzas123")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CREDIT_OPERATION_NOT_FOUND"));
    }

    private String validBatch() throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "contractVersion", "credit-history-v1",
                "correlationId", UUID.randomUUID().toString(),
                "entries", List.of(purchase("sec-ok-" + UUID.randomUUID()))));
    }

    private static Map<String, Object> purchase(String reference) {
        return Map.of("customerId", UUID.randomUUID().toString(), "sourceReference", reference,
                "type", "PURCHASE", "occurredAt", Instant.now().minusSeconds(3600).toString(),
                "amount", 100.00, "critical", false);
    }

    private String token(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }
}
