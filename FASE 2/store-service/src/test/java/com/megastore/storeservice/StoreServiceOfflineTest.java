package com.megastore.storeservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Con los clientes reales (sin dobles) y el central inalcanzable: la
 * tienda arranca, responde y reporta la falta de conexión en vez de caerse.
 */
@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "store.sync.enabled=false",
        "store.central.url=http://127.0.0.1:9",
        "store.monolith.url=http://127.0.0.1:9"})
class StoreServiceOfflineTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17.11-alpine3.23"))
            .withDatabaseName("store_offline_test")
            .withUsername("store_test")
            .withPassword("test_only_password");

    @DynamicPropertySource
    static void infrastructureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    MockMvc mockMvc;

    @Test
    void storeStartsAndReportsCentralUnavailable() throws Exception {
        mockMvc.perform(get("/api/store/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("POS-01"))
                .andExpect(jsonPath("$.centralReachable").value(false));
        mockMvc.perform(post("/api/store/sync"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("CENTRAL_UNAVAILABLE"));
        mockMvc.perform(get("/api/store/web-cart").param("username", "ana"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("WEB_STORE_UNAVAILABLE"));
        mockMvc.perform(get("/api/store/sync-log"))
                .andExpect(jsonPath("$[0].direction").value("BAJADA"))
                .andExpect(jsonPath("$[0].result").value("ERROR"));
    }
}
