package com.megastore.sgologic.credit;

import com.megastore.sgologic.credit.api.CreditOperationResult;
import com.megastore.sgologic.credit.api.CreditPort;
import com.megastore.sgologic.credit.api.CreditRequest;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * EDS3-04: la explicación de una obligación se reconstruye con quién, cuándo,
 * regla, resultado, razón, entradas y correlación, y solo Finanzas la consulta
 * (rol fijado en SecurityConfig y verificado en CreditSecurityTest).
 */
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = "sgo.security.enabled=true")
class CreditExplanationIntegrationTest {

    private static final UUID ELIGIBLE_CUSTOMER =
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17.11-alpine3.23"))
            .withDatabaseName("sgo_credit_explanation_test")
            .withUsername("sgo_test")
            .withPassword("test_only_password");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired CreditPort credit;
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void resetCreditLedger() {
        jdbc.update("DELETE FROM credit_operations");
        jdbc.update("DELETE FROM credit_decisions");
        jdbc.update("UPDATE credit_customer_profiles SET available_credit = 2500.00, version = 0 "
                + "WHERE customer_id = ?", ELIGIBLE_CUSTOMER);
    }

    @Test
    void financeReconstructsWhoWhenRuleResultAndReasonOfAGrantedCredit() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        CreditOperationResult granted = credit.requestCredit(new CreditRequest(
                orderId, ELIGIBLE_CUSTOMER, new BigDecimal("399.00"), "GTQ",
                correlationId, "explain-granted-" + UUID.randomUUID()));

        JsonNode explanation = getExplanation(granted.creditOperationId().toString());
        JsonNode operation = explanation.get("operation");
        JsonNode decision = explanation.get("decision");

        assertThat(operation.get("id").asText()).isEqualTo(granted.creditOperationId().toString());
        assertThat(operation.get("orderId").asText()).isEqualTo(orderId.toString());
        assertThat(operation.get("customerId").asText()).isEqualTo(ELIGIBLE_CUSTOMER.toString());
        assertThat(operation.get("status").asText()).isEqualTo("GRANTED");
        assertThat(operation.get("amount").decimalValue()).isEqualByComparingTo("399.00");
        assertThat(operation.get("currency").asText()).isEqualTo("GTQ");
        assertThat(operation.get("ruleVersion").asText()).isEqualTo("credit-rules-v1");
        assertThat(operation.get("reasonCode").asText()).isNotBlank();
        assertThat(LocalDate.parse(operation.get("dueDate").asText()))
                .isEqualTo(LocalDate.now(ZoneOffset.UTC).plusDays(30));
        assertThat(operation.get("createdAt").asText()).isNotBlank();
        assertThat(operation.get("correlationId").asText()).isEqualTo(correlationId.toString());

        assertThat(decision.get("id").asText()).isEqualTo(granted.decisionId().toString());
        assertThat(decision.get("eligible").asBoolean()).isTrue();
        assertThat(decision.get("requestedAmount").decimalValue()).isEqualByComparingTo("399.00");
        assertThat(decision.get("ruleVersion").asText()).isEqualTo("credit-rules-v1");
        assertThat(decision.get("reasonCode").asText()).isNotBlank();
        assertThat(decision.get("inputsSnapshot").asText()).isNotBlank();
        assertThat(decision.get("evaluatedAt").asText()).isNotBlank();
        assertThat(decision.get("correlationId").asText()).isEqualTo(correlationId.toString());
    }

    @Test
    void rejectedCreditExplainsTheReasonAndHasNoDueDate() throws Exception {
        CreditOperationResult rejected = credit.requestCredit(new CreditRequest(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("2500.01"), "GTQ",
                UUID.randomUUID(), "explain-rejected-" + UUID.randomUUID()));

        JsonNode explanation = getExplanation(rejected.creditOperationId().toString());
        JsonNode operation = explanation.get("operation");
        JsonNode decision = explanation.get("decision");

        assertThat(operation.get("status").asText()).isEqualTo("REJECTED");
        // La app omite los campos nulos en la serialización: ausente o nulo.
        assertThat(operation.get("dueDate") == null || operation.get("dueDate").isNull()).isTrue();
        assertThat(operation.get("reasonCode").asText()).isNotBlank();
        assertThat(decision.get("eligible").asBoolean()).isFalse();
        assertThat(decision.get("reasonCode").asText()).isEqualTo(rejected.publicReasonCode());
        assertThat(decision.get("inputsSnapshot").asText()).isNotBlank();
    }

    private JsonNode getExplanation(String operationId) throws Exception {
        String content = mockMvc.perform(get("/api/v1/credit/operations/" + operationId)
                        .header("Authorization", "Bearer " + token("finanzas@megastore.com", "Finanzas123")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(content);
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
