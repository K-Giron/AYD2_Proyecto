package com.megastore.inventoryservice;

import com.megastore.inventoryservice.article.CreateArticleRequest;
import com.megastore.inventoryservice.finance.CreateInvoiceRequest;
import com.megastore.inventoryservice.finance.InvoiceType;
import com.megastore.inventoryservice.logistics.ShipmentStatus;
import com.megastore.inventoryservice.logistics.UpdateShipmentStatusRequest;
import com.megastore.inventoryservice.stock.RegisterStockMovementRequest;
import com.megastore.inventoryservice.stock.StockMovementType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de extremo a extremo del almacén central contra PostgreSQL real:
 * login por rol, artículo, entrada/salida con folio hacia un punto de venta,
 * cuenta por pagar/cobrar y envío generado automáticamente desde el folio
 * de salida. El punto de venta y el almacén usados aquí son los sembrados
 * por V5/V6 (POS-01, "Tienda Centro", Almacén Central).
 */
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = "inventory.pos-events.enabled=false")
class InventoryServiceIntegrationTest {

    private static final UUID SEEDED_POINT_OF_SALE = UUID.fromString("a2000000-0000-0000-0000-000000000001");

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17.11-alpine3.23"))
            .withDatabaseName("inventory_service_test")
            .withUsername("inv_test")
            .withPassword("test_only_password");

    @DynamicPropertySource
    static void infrastructureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    private String login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(body).get("accessToken").asText();
    }

    @Test
    void registersEntradaAndSalidaWithFolioAndRejectsExcessiveSalida() throws Exception {
        String almacenToken = login("almacen@almacencentral.local", "Almacen123");

        mockMvc.perform(get("/api/v1/warehouses").header("Authorization", almacenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("ALM-CENTRAL"));
        mockMvc.perform(get("/api/v1/points-of-sale").header("Authorization", almacenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'POS-01')].warehouseCode").value("ALM-POS-01"));

        CreateArticleRequest createRequest = new CreateArticleRequest(
                "MS-INT-001", "Camisa de prueba", "Artículo de integración", new BigDecimal("150.00"));
        String articleJson = mockMvc.perform(post("/api/v1/articles")
                        .header("Authorization", almacenToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID articleId = UUID.fromString(objectMapper.readTree(articleJson).get("id").asText());

        RegisterStockMovementRequest entrada =
                new RegisterStockMovementRequest(articleId, StockMovementType.ENTRADA, 20, "Recepción inicial", null, null);
        mockMvc.perform(post("/api/v1/stock-movements")
                        .header("Authorization", almacenToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(entrada)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.folio", startsWith("ENT-")))
                .andExpect(jsonPath("$.articleSku").value("MS-INT-001"))
                .andExpect(jsonPath("$.quantityOnHandAfter").value(20));

        RegisterStockMovementRequest salida =
                new RegisterStockMovementRequest(articleId, StockMovementType.SALIDA, 8, "Envío a tienda", SEEDED_POINT_OF_SALE, null);
        String salidaJson = mockMvc.perform(post("/api/v1/stock-movements")
                        .header("Authorization", almacenToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(salida)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.folio", startsWith("SAL-")))
                .andExpect(jsonPath("$.quantityOnHandAfter").value(12))
                .andReturn().getResponse().getContentAsString();
        String salidaFolio = objectMapper.readTree(salidaJson).get("folio").asText();

        mockMvc.perform(get("/api/v1/stock").header("Authorization", almacenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.articleId == '" + articleId + "')].quantityOnHand").value(12));

        RegisterStockMovementRequest excessiveSalida =
                new RegisterStockMovementRequest(articleId, StockMovementType.SALIDA, 999, null, SEEDED_POINT_OF_SALE, null);
        mockMvc.perform(post("/api/v1/stock-movements")
                        .header("Authorization", almacenToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(excessiveSalida)))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("INSUFFICIENT_STOCK")));

        // Una salida sin punto de venta se rechaza antes de tocar la existencia.
        RegisterStockMovementRequest salidaSinDestino =
                new RegisterStockMovementRequest(articleId, StockMovementType.SALIDA, 1, null, null, null);
        mockMvc.perform(post("/api/v1/stock-movements")
                        .header("Authorization", almacenToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(salidaSinDestino)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("POINT_OF_SALE_REQUIRED")));

        // Vista general: 12 en el central y 8 en el almacén propio de POS-01 (total de la red 20).
        mockMvc.perform(get("/api/v1/stock-overview").header("Authorization", almacenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locations[0].code").value("CENTRAL"))
                .andExpect(jsonPath("$.locations[?(@.code == 'POS-01')].warehouseCode").value("ALM-POS-01"))
                .andExpect(jsonPath("$.articles[?(@.articleId == '" + articleId + "')].quantities.CENTRAL").value(12))
                .andExpect(jsonPath("$.articles[?(@.articleId == '" + articleId + "')].quantities['POS-01']").value(8))
                .andExpect(jsonPath("$.articles[?(@.articleId == '" + articleId + "')].total").value(20));

        // El folio de salida debe haber generado automáticamente un envío PENDIENTE.
        String logisticaToken = login("logistica@almacencentral.local", "Logistica123");
        mockMvc.perform(get("/api/v1/shipments/" + salidaFolio).header("Authorization", logisticaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDIENTE"))
                .andExpect(jsonPath("$.pointOfSaleCode").value("POS-01"))
                .andExpect(jsonPath("$.warehouseCode").value("ALM-POS-01"));

        mockMvc.perform(patch("/api/v1/shipments/" + salidaFolio + "/status")
                        .header("Authorization", logisticaToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new UpdateShipmentStatusRequest(ShipmentStatus.EN_TRANSITO))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EN_TRANSITO"));

        // Un rol equivocado no puede avanzar un envío.
        mockMvc.perform(patch("/api/v1/shipments/" + salidaFolio + "/status")
                        .header("Authorization", almacenToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new UpdateShipmentStatusRequest(ShipmentStatus.ENTREGADO))))
                .andExpect(status().isForbidden());

        // Logística también puede registrar folios de entrada/salida (despachar es una acción de logística).
        RegisterStockMovementRequest entradaPorLogistica =
                new RegisterStockMovementRequest(articleId, StockMovementType.ENTRADA, 5, "Reposición", null, null);
        mockMvc.perform(post("/api/v1/stock-movements")
                        .header("Authorization", logisticaToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(entradaPorLogistica)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.folio", startsWith("ENT-")));

        // Idempotencia por externalReference: dos llamadas con la misma referencia
        // (ej. un reintento de red de una integración) no duplican el efecto.
        String externalReference = "WEB:" + UUID.randomUUID() + ":" + articleId;
        RegisterStockMovementRequest ventaWeb = new RegisterStockMovementRequest(
                articleId, StockMovementType.SALIDA, 2, "Venta web", SEEDED_POINT_OF_SALE, externalReference);
        String primeraVentaJson = mockMvc.perform(post("/api/v1/stock-movements")
                        .header("Authorization", logisticaToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(ventaWeb)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.folio", startsWith("SAL-")))
                .andReturn().getResponse().getContentAsString();
        String primeraVentaFolio = objectMapper.readTree(primeraVentaJson).get("folio").asText();
        int existenciaTrasPrimeraVenta = objectMapper.readTree(primeraVentaJson).get("quantityOnHandAfter").asInt();

        mockMvc.perform(post("/api/v1/stock-movements")
                        .header("Authorization", logisticaToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(ventaWeb)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.folio").value(primeraVentaFolio))
                .andExpect(jsonPath("$.externalReference").value(externalReference));

        mockMvc.perform(get("/api/v1/stock").header("Authorization", almacenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.articleId == '" + articleId + "')].quantityOnHand")
                        .value(existenciaTrasPrimeraVenta));

        // Finanzas: cuenta por cobrar a la tienda a partir del mismo folio.
        String finanzasToken = login("finanzas@almacencentral.local", "Finanzas123");
        CreateInvoiceRequest invoiceRequest = new CreateInvoiceRequest(InvoiceType.RECEIVABLE, "Tienda 5",
                "Mercadería enviada", new BigDecimal("1200.00"), salidaFolio, LocalDate.now().plusDays(15));
        String invoiceJson = mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", finanzasToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invoiceRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        String invoiceId = objectMapper.readTree(invoiceJson).get("id").asText();

        mockMvc.perform(patch("/api/v1/invoices/" + invoiceId + "/pay").header("Authorization", finanzasToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        // Finanzas no puede registrar folios (sigue siendo exclusivo de Almacén/Logística).
        mockMvc.perform(post("/api/v1/stock-movements")
                        .header("Authorization", finanzasToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(entradaPorLogistica)))
                .andExpect(status().isForbidden());

        // Sin token, ninguna llamada a la API pasa.
        mockMvc.perform(get("/api/v1/stock")).andExpect(status().isUnauthorized());
    }
}
