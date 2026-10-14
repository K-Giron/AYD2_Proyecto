package com.payklik.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PayKlik contra PostgreSQL real: llave de comercio, cobro idempotente,
 * rechazo de PAN, emisor que declina, consulta para conciliar y servicio
 * fuera de línea.
 */
@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = "payklik.api-key=test-key")
class PayKlikIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17.11-alpine3.23"))
            .withDatabaseName("payklik_test")
            .withUsername("payklik_test")
            .withPassword("test_only_password");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM transactions");
        jdbc.update("UPDATE operation_settings SET mode = 'NORMAL'");
    }

    private static String charge(String key, String reference) {
        return """
                {"idempotencyKey":"%s","orderReference":"order-1","amount":749.00,"currency":"gtq","paymentReference":"%s"}
                """.formatted(key, reference);
    }

    private void mode(String mode) throws Exception {
        mvc.perform(put("/api/portal/operation").contentType(MediaType.APPLICATION_JSON)
                .content("{\"mode\":\"" + mode + "\",\"updatedBy\":\"operador\"}")).andExpect(status().isOk());
    }

    @Test
    void requiresTheMerchantKey() throws Exception {
        mvc.perform(post("/api/v1/authorizations").contentType(MediaType.APPLICATION_JSON).content(charge("k-1", "tok_a")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sameKeyIsChargedOnlyOnce() throws Exception {
        mvc.perform(post("/api/v1/authorizations").header("X-Api-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON).content(charge("k-2", "tok_payklik_abc")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.approved").value(true))
                .andExpect(jsonPath("$.method").value("TARJETA"))
                .andExpect(jsonPath("$.authorizationCode").value(matchesPattern("PK-\\d{8}")));
        mvc.perform(post("/api/v1/authorizations").header("X-Api-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON).content(charge("k-2", "tok_payklik_abc")))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM transactions", Integer.class)).isEqualTo(1);
        // Conciliación por llave.
        mvc.perform(get("/api/v1/authorizations/k-2").header("X-Api-Key", "test-key"))
                .andExpect(jsonPath("$.status").value("APROBADA"));
        mvc.perform(get("/api/v1/authorizations/no-existe").header("X-Api-Key", "test-key"))
                .andExpect(status().isNotFound());
    }

    @Test
    void neverAcceptsACardNumber() throws Exception {
        mvc.perform(post("/api/v1/authorizations").header("X-Api-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON).content(charge("k-3", "4111 1111 1111 1111")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SENSITIVE_DATA"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM transactions", Integer.class)).isZero();
    }

    @Test
    void issuerDeclinesAndOutageAnswers503() throws Exception {
        mode("RECHAZAR");
        mvc.perform(post("/api/v1/authorizations").header("X-Api-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON).content(charge("k-4", "TRF-MS-AB12CD34")))
                .andExpect(jsonPath("$.approved").value(false))
                .andExpect(jsonPath("$.method").value("TRANSFERENCIA"))
                .andExpect(jsonPath("$.rejectionCode").value("DECLINADA_POR_EMISOR"));
        mode("FUERA_DE_SERVICIO");
        mvc.perform(post("/api/v1/authorizations").header("X-Api-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON).content(charge("k-5", "tok_x")))
                .andExpect(status().isServiceUnavailable());
    }
}
