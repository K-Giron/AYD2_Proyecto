package com.logiexpress.carrier;

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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LogiExpress contra PostgreSQL real: llave de comercio, emisión idempotente
 * de guías, recolección y entrega (domicilio y tienda de retiro), rastreo
 * incremental y servicio fuera de línea.
 */
@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "logiexpress.api-key=test-key")
class LogiExpressIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17.11-alpine3.23"))
            .withDatabaseName("logiexpress_test")
            .withUsername("logiexpress_test")
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
        jdbc.update("DELETE FROM guide_events");
        jdbc.update("DELETE FROM guides");
        jdbc.update("UPDATE operation_settings SET mode = 'NORMAL'");
    }

    private static String guide(String reference, String pickupPoint) {
        return """
                {"merchantReference":"%s","orderReference":"order-1","origin":"Almacén CD-WEB",
                 "destination":"Recoger en Tienda Centro (POS-01)"%s}
                """.formatted(reference, pickupPoint == null ? "" : ",\"pickupPoint\":\"" + pickupPoint + "\"");
    }

    @Test
    void merchantApiRequiresTheApiKey() throws Exception {
        mvc.perform(post("/api/v1/guides").contentType(MediaType.APPLICATION_JSON).content(guide("s-1", null)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_API_KEY"));
        mvc.perform(post("/api/v1/guides").header("X-Api-Key", "otra").contentType(MediaType.APPLICATION_JSON)
                        .content(guide("s-1", null)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void issuingIsIdempotentByMerchantReference() throws Exception {
        String first = mvc.perform(post("/api/v1/guides").header("X-Api-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON).content(guide("s-2", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.trackingNumber").value(matchesPattern("LGX-\\d{8}")))
                .andExpect(jsonPath("$.status").value("CREADA"))
                .andReturn().getResponse().getContentAsString();
        String number = first.replaceAll(".*\"trackingNumber\":\"(LGX-\\d{8})\".*", "$1");
        mvc.perform(post("/api/v1/guides").header("X-Api-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON).content(guide("s-2", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingNumber").value(number));
    }

    @Test
    void pickupGuideEndsInTheStoreAndHomeGuideWithTheCustomer() throws Exception {
        String store = issue("s-3", "POS-01");
        String home = issue("s-4", null);
        for (String number : new String[]{store, home}) {
            mvc.perform(post("/api/portal/guides/" + number + "/pickup").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"courier\":\"juan\"}"))
                    .andExpect(jsonPath("$.status").value("EN_TRANSITO"));
        }
        mvc.perform(post("/api/portal/guides/" + store + "/deliver").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courier\":\"juan\"}"))
                .andExpect(jsonPath("$.status").value("EN_PUNTO_DE_RETIRO"));
        mvc.perform(post("/api/portal/guides/" + home + "/deliver").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courier\":\"juan\",\"receivedBy\":\"Ana\"}"))
                .andExpect(jsonPath("$.status").value("ENTREGADA"))
                .andExpect(jsonPath("$.events", hasSize(3)));
        // No se entrega dos veces.
        mvc.perform(post("/api/portal/guides/" + home + "/deliver").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courier\":\"juan\"}"))
                .andExpect(status().isConflict());
        // Rastreo incremental del comercio: ambas guías con su historial.
        mvc.perform(get("/api/v1/guides").header("X-Api-Key", "test-key").param("updatedSince", "2020-01-01T00:00:00Z"))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void outOfServiceRejectsMerchantsWith503() throws Exception {
        mvc.perform(put("/api/portal/operation").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"FUERA_DE_SERVICIO\",\"updatedBy\":\"supervisor\"}"))
                .andExpect(jsonPath("$.mode").value("FUERA_DE_SERVICIO"));
        mvc.perform(post("/api/v1/guides").header("X-Api-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON).content(guide("s-5", null)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"));
    }

    private String issue(String reference, String pickupPoint) throws Exception {
        String body = mvc.perform(post("/api/v1/guides").header("X-Api-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON).content(guide(reference, pickupPoint)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return body.replaceAll(".*\"trackingNumber\":\"(LGX-\\d{8})\".*", "$1");
    }
}
