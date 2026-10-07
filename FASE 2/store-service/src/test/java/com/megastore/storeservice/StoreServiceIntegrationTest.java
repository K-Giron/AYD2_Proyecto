package com.megastore.storeservice;

import com.megastore.storeservice.inventory.IncomingReceipt;
import com.megastore.storeservice.inventory.InventoryService;
import com.megastore.storeservice.sync.CentralClient;
import com.megastore.storeservice.sync.CentralRejectedException;
import com.megastore.storeservice.sync.CentralUnavailableException;
import com.megastore.storeservice.sync.SyncService;
import com.megastore.storeservice.webcart.MonolithClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La tienda contra PostgreSQL real, con el central y la web simulados:
 * recepción idempotente, venta CF/NIT, falta de existencia, venta sin
 * conexión que sube después, traslado rechazado y carrito web opcional.
 */
@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = "store.sync.enabled=false")
class StoreServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17.11-alpine3.23"))
            .withDatabaseName("store_test")
            .withUsername("store_test")
            .withPassword("test_only_password");

    @DynamicPropertySource
    static void infrastructureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @MockitoBean
    CentralClient central;

    @MockitoBean
    MonolithClient monolith;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    SyncService syncService;

    @Autowired
    InventoryService inventoryService;

    @Autowired
    JdbcTemplate jdbc;

    private final UUID camisa = UUID.fromString("11111111-1111-1111-1111-111111111101");

    @BeforeEach
    void cleanStore() {
        jdbc.execute("TRUNCATE sale_lines, sales, transfers, store_receipts, store_stock, store_articles, sync_log, stock_events");
        reset(central, monolith);
        ship("POS-ENT-000001", "SAL-000001", 10);
        inventoryService.confirmShipment("SAL-000001", "maria");
    }

    /** El central envía mercadería (queda en tránsito hasta que la tienda confirma el folio). */
    private void ship(String folio, String shipmentFolio, int quantity) {
        when(central.receipts(any())).thenReturn(List.of(new IncomingReceipt(folio, shipmentFolio, "CENTRAL", camisa,
                "MS-CAM-101", "Camisa", "Manga larga, algodón azul", new BigDecimal("150.00"), quantity, Instant.parse("2026-10-05T10:00:00Z"))));
        syncService.pullReceipts(false);
    }

    private String sale(String taxId, String customerName, String payment, String cash, int quantity) {
        return """
                {"seller":"maria","taxId":%s,"customerName":%s,"paymentMethod":"%s","cashReceived":%s,
                 "lines":[{"articleId":"%s","quantity":%d}]}
                """.formatted(taxId == null ? "null" : "\"" + taxId + "\"",
                customerName == null ? "null" : "\"" + customerName + "\"", payment, cash, camisa, quantity);
    }

    @Test
    void shipmentStaysInTransitUntilTheStoreConfirmsItsFolio() throws Exception {
        ship("POS-ENT-000002", "SAL-000007", 5);
        ship("POS-ENT-000002", "SAL-000007", 5);

        mockMvc.perform(get("/api/store/articles/by-sku/ms-cam-101"))
                .andExpect(jsonPath("$.quantity").value(10));
        mockMvc.perform(get("/api/store/stock"))
                .andExpect(jsonPath("$[0].inTransit").value(5));
        mockMvc.perform(get("/api/store/receiving/sal-000007"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EN_TRANSITO"))
                .andExpect(jsonPath("$.origin").value("CENTRAL"))
                .andExpect(jsonPath("$.units").value(5))
                .andExpect(jsonPath("$.lines[0].sku").value("MS-CAM-101"));

        mockMvc.perform(post("/api/store/receiving/SAL-000007/confirm").contentType("application/json")
                        .content("{\"seller\":\"lucia\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECIBIDO"))
                .andExpect(jsonPath("$.receivedBy").value("lucia"));
        mockMvc.perform(post("/api/store/receiving/SAL-000007/confirm").contentType("application/json")
                        .content("{\"seller\":\"lucia\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FOLIO_ALREADY_RECEIVED"));

        assertThat(inventoryService.quantityOf(camisa)).isEqualTo(15);
        mockMvc.perform(get("/api/store/receiving/SAL-999999"))
                .andExpect(jsonPath("$.status").value("NO_ENCONTRADO"));
    }

    @Test
    void manualSearchFindsByCodeNameOrDescription() throws Exception {
        mockMvc.perform(get("/api/store/articles/search").param("q", "algod"))
                .andExpect(jsonPath("$[0].sku").value("MS-CAM-101"))
                .andExpect(jsonPath("$[0].description").value("Manga larga, algodón azul"))
                .andExpect(jsonPath("$[0].quantity").value(10));
        mockMvc.perform(get("/api/store/articles/search").param("q", "CAMI"))
                .andExpect(jsonPath("$[0].name").value("Camisa"));
        mockMvc.perform(get("/api/store/articles/search").param("q", "cam-1"))
                .andExpect(jsonPath("$[0].sku").value("MS-CAM-101"));
        mockMvc.perform(get("/api/store/articles/search").param("q", "100%"))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/store/articles/search").param("q", "x"))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void invoicesAreListedByDateAndCanBeReprinted() throws Exception {
        when(central.uploadInvoice(any())).thenReturn(new CentralClient.InvoiceReceipt("FT-000010", "x", Instant.now()));
        String body = mockMvc.perform(post("/api/store/sales").contentType("application/json")
                        .content(sale("1234567-8", "Ana López", "TARJETA", "null", 1)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String saleId = body.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
        java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneId.of("America/Guatemala"));

        mockMvc.perform(get("/api/store/sales"))
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/store/sales").param("from", today.toString()).param("to", today.toString()).param("q", "ana"))
                .andExpect(jsonPath("$[0].customerName").value("Ana López"));
        mockMvc.perform(get("/api/store/sales").param("from", today.minusDays(3).toString()).param("to", today.minusDays(1).toString()))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/store/sales").param("from", today.toString()).param("to", today.minusDays(1).toString()))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/store/sales/" + saleId + "/reprint"))
                .andExpect(jsonPath("$.reprintCount").value(1));
        mockMvc.perform(post("/api/store/sales/" + saleId + "/reprint"))
                .andExpect(jsonPath("$.reprintCount").value(2))
                .andExpect(jsonPath("$.lastReprintedAt").exists());
    }

    @Test
    void webOrderEventsFromSgoLogicAreStoredOnceAndPickupOrdersAreFlagged() throws Exception {
        jdbc.execute("TRUNCATE pos_events");
        UUID orderId = UUID.randomUUID();
        String event = """
                {"eventId":"%s","eventType":"ORDER_PREPARING","orderId":"%s","storeNumber":1,
                 "payload":"{\\"orderId\\":\\"%s\\",\\"status\\":\\"PREPARING\\",\\"total\\":749.00,\\"items\\":[{\\"name\\":\\"Tenis\\",\\"quantity\\":1}],\\"destination\\":\\"Recoger en Tienda Centro (POS-01)\\",\\"pickupStore\\":\\"POS-01\\"}"}
                """.formatted(UUID.randomUUID(), orderId, orderId);
        mockMvc.perform(post("/api/store/pos-events").contentType("application/json").content(event))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/store/pos-events").contentType("application/json").content(event))
                .andExpect(status().isOk());

        assertThat(jdbc.queryForObject("select count(*) from pos_events", Long.class)).isEqualTo(1L);
        mockMvc.perform(get("/api/store/pos-events"))
                .andExpect(jsonPath("$[0].orderId").value(orderId.toString()))
                .andExpect(jsonPath("$[0].status").value("PREPARING"))
                .andExpect(jsonPath("$[0].pickupHere").value(true))
                .andExpect(jsonPath("$[0].total").value("749.00"))
                .andExpect(jsonPath("$[0].items[0]").value("1 × Tenis"));
    }

    private String stockEvent(UUID eventId, String movement, int delta, int quantityAfter) {
        return """
                {"eventId":"%s","posCode":"POS-01","articleId":"%s","sku":"MS-CAM-101","name":"Camisa",
                 "movement":"%s","delta":%d,"quantityAfter":%d,"folio":"POS-VTA-000009","reference":"FT-000009"}
                """.formatted(eventId, camisa, movement, delta, quantityAfter);
    }

    @Test
    void stockEventFromTheCentralIsLoggedAndComparedWithTheStore() throws Exception {
        UUID eventId = UUID.randomUUID();
        mockMvc.perform(post("/api/store/stock-events").contentType("application/json").content(stockEvent(eventId, "VENTA", -2, 10)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/store/stock-events").contentType("application/json").content(stockEvent(eventId, "VENTA", -2, 10)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/store/stock-events").contentType("application/json").content(stockEvent(UUID.randomUUID(), "VENTA", -1, 7)))
                .andExpect(status().isOk());

        // Repetido no duplica; 10 en el central = 10 en la tienda cuadra, 7 no.
        assertThat(jdbc.queryForList("select result from sync_log where kind = 'EVENTO_STOCK' order by occurred_at", String.class))
                .containsExactly("OK", "ERROR");
        assertThat(jdbc.queryForObject("select detail from sync_log where kind = 'EVENTO_STOCK' and result = 'ERROR'", String.class))
                .contains("Existencia en el central: 7; en la tienda: 10", "No cuadra");
    }

    @Test
    void pickupIsHandedOverOnlyWhenTheWebStoreAccepts() throws Exception {
        UUID orderId = UUID.randomUUID();
        mockMvc.perform(post("/api/store/pos-events/" + orderId + "/handoff").contentType("application/json")
                        .content("{\"seller\":\"maria\",\"receivedBy\":\"Ana López DPI 1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delivered").value(true));
        verify(monolith).handOverPickup(orderId, "POS-01", "Ana López DPI 1234");

        org.mockito.Mockito.doThrow(new com.megastore.storeservice.shared.StoreBusinessException(
                        org.springframework.http.HttpStatus.CONFLICT, "WEB_STORE_CONFLICT", "El pedido todavía no llega a la tienda."))
                .when(monolith).handOverPickup(any(), anyString(), anyString());
        mockMvc.perform(post("/api/store/pos-events/" + orderId + "/handoff").contentType("application/json")
                        .content("{\"seller\":\"maria\",\"receivedBy\":\"Ana\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El pedido todavía no llega a la tienda."));
        assertThat(jdbc.queryForList("select result from sync_log where kind = 'ENTREGA_PEDIDO' order by occurred_at", String.class))
                .containsExactly("OK", "ERROR");
    }

    @Test
    void articleNeverSentByCentralCannotBeSold() throws Exception {
        UUID unknown = UUID.randomUUID();
        when(central.receipts(any())).thenReturn(List.of(new IncomingReceipt("POS-ENT-000003", "TRA-000004", "POS-02",
                unknown, "MS-ZAP-103", "Zapatos", null, new BigDecimal("300.00"), 2, Instant.parse("2026-10-05T11:00:00Z"))));
        syncService.pullReceipts(false);

        mockMvc.perform(get("/api/store/articles/by-sku/MS-ZAP-103")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/store/receiving/TRA-000004")).andExpect(jsonPath("$.origin").value("POS-02"));
        mockMvc.perform(post("/api/store/receiving/TRA-000004/confirm").contentType("application/json")
                .content("{\"seller\":\"lucia\"}")).andExpect(status().isOk());
        mockMvc.perform(get("/api/store/articles/by-sku/MS-ZAP-103")).andExpect(jsonPath("$.quantity").value(2));
    }

    @Test
    void cashSaleAsConsumidorFinalDecreasesStockGivesChangeAndUploadsInvoice() throws Exception {
        when(central.uploadInvoice(any())).thenReturn(new CentralClient.InvoiceReceipt("FT-000001", "TKT-000001", Instant.now()));

        mockMvc.perform(post("/api/store/sales").contentType("application/json")
                        .content(sale(null, null, "EFECTIVO", "500.00", 3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.folio", startsWith("TKT-")))
                .andExpect(jsonPath("$.taxId").value("CF"))
                .andExpect(jsonPath("$.customerName").value("Consumidor Final"))
                .andExpect(jsonPath("$.total").value(450.00))
                .andExpect(jsonPath("$.changeGiven").value(50.00))
                .andExpect(jsonPath("$.syncStatus").value("ENVIADA"))
                .andExpect(jsonPath("$.centralFolio").value("FT-000001"));

        assertThat(inventoryService.quantityOf(camisa)).isEqualTo(7);
    }

    @Test
    void invoiceWithNitNeedsCustomerNameAndValidNit() throws Exception {
        mockMvc.perform(post("/api/store/sales").contentType("application/json")
                        .content(sale("1234567-8", null, "TARJETA", "null", 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CUSTOMER_NAME_REQUIRED"));
        mockMvc.perform(post("/api/store/sales").contentType("application/json")
                        .content(sale("ABC", "Ana López", "TARJETA", "null", 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TAX_ID"));
        when(central.uploadInvoice(any())).thenReturn(new CentralClient.InvoiceReceipt("FT-000002", "x", Instant.now()));
        mockMvc.perform(post("/api/store/sales").contentType("application/json")
                        .content(sale("1234567-k", "Ana López", "TARJETA", "null", 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taxId").value("1234567-K"))
                .andExpect(jsonPath("$.cashReceived").doesNotExist());
    }

    @Test
    void saleWithoutEnoughStockRegistersNothing() throws Exception {
        mockMvc.perform(post("/api/store/sales").contentType("application/json")
                        .content(sale(null, null, "EFECTIVO", "5000.00", 11)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
        mockMvc.perform(post("/api/store/sales").contentType("application/json")
                        .content(sale(null, null, "EFECTIVO", "100.00", 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CASH_NOT_ENOUGH"));

        assertThat(inventoryService.quantityOf(camisa)).isEqualTo(10);
        assertThat(jdbc.queryForObject("select count(*) from sales", Long.class)).isZero();
        verify(central, never()).uploadInvoice(any());
    }

    @Test
    void saleWorksWithoutCentralAndUploadsWhenItComesBack() throws Exception {
        when(central.uploadInvoice(any())).thenThrow(new CentralUnavailableException("Sin conexión con el almacén central.", null));

        mockMvc.perform(post("/api/store/sales").contentType("application/json")
                        .content(sale(null, null, "TARJETA", "null", 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.syncStatus").value("PENDIENTE"))
                .andExpect(jsonPath("$.syncAttempts").value(1));
        assertThat(inventoryService.quantityOf(camisa)).isEqualTo(8);

        reset(central);
        when(central.uploadInvoice(any())).thenReturn(new CentralClient.InvoiceReceipt("FT-000009", "x", Instant.now()));
        syncService.pushPending();

        mockMvc.perform(get("/api/store/sales"))
                .andExpect(jsonPath("$[0].syncStatus").value("ENVIADA"))
                .andExpect(jsonPath("$[0].centralFolio").value("FT-000009"));
        mockMvc.perform(get("/api/store/sync-log"))
                .andExpect(jsonPath("$[0].result").value("OK"))
                .andExpect(jsonPath("$[1].result").value("ERROR"));
    }

    @Test
    void transferRejectedByCentralGivesTheStockBack() throws Exception {
        when(central.registerTransfer(any())).thenThrow(new CentralRejectedException(409, "No alcanza la existencia."));

        mockMvc.perform(post("/api/store/transfers").contentType("application/json")
                        .content("""
                                {"articleId":"%s","quantity":4,"destination":"central","seller":"maria"}
                                """.formatted(camisa)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.folio", startsWith("TRS-")))
                .andExpect(jsonPath("$.destination").value("CENTRAL"))
                .andExpect(jsonPath("$.syncStatus").value("RECHAZADO"));

        assertThat(inventoryService.quantityOf(camisa)).isEqualTo(10);
    }

    @Test
    void transferAcceptedLeavesTheStore() throws Exception {
        when(central.registerTransfer(any())).thenReturn(new CentralClient.TransferReceipt("TRA-000001", "TRS-000001", "POS-ENT-000002"));

        mockMvc.perform(post("/api/store/transfers").contentType("application/json")
                        .content("""
                                {"articleId":"%s","quantity":4,"destination":"POS-02","seller":"maria"}
                                """.formatted(camisa)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.syncStatus").value("ENVIADA"))
                .andExpect(jsonPath("$.destinationFolio").value("POS-ENT-000002"));
        mockMvc.perform(post("/api/store/transfers").contentType("application/json")
                        .content("""
                                {"articleId":"%s","quantity":1,"destination":"POS-01","seller":"maria"}
                                """.formatted(camisa)))
                .andExpect(status().isBadRequest());

        assertThat(inventoryService.quantityOf(camisa)).isEqualTo(6);
    }

    @Test
    void webCartIsOptionalAndIsClosedAfterCheckout() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID cartId = UUID.randomUUID();
        when(monolith.findCustomer("ana")).thenReturn(Optional.of(new MonolithClient.Customer(customerId, "ana", "Ana López")));
        when(monolith.activeCart(customerId)).thenReturn(Optional.of(new MonolithClient.Cart(cartId, "ACTIVE", "WEB",
                List.of(new MonolithClient.CartItem(UUID.randomUUID(), "camisa", new BigDecimal("149.00"), 2),
                        new MonolithClient.CartItem(UUID.randomUUID(), "Zapatos", new BigDecimal("300.00"), 1)),
                new BigDecimal("598.00"))));

        mockMvc.perform(get("/api/store/web-cart").param("username", "ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").value(cartId.toString()))
                .andExpect(jsonPath("$.lines[0].sku").value("MS-CAM-101"))
                .andExpect(jsonPath("$.lines[0].price").value(150.00))
                .andExpect(jsonPath("$.lines[0].quantity").value(2))
                .andExpect(jsonPath("$.unavailable[0]", startsWith("Zapatos")));

        when(central.uploadInvoice(any())).thenReturn(new CentralClient.InvoiceReceipt("FT-000003", "x", Instant.now()));
        mockMvc.perform(post("/api/store/sales").contentType("application/json")
                        .content("""
                                {"seller":"maria","paymentMethod":"TARJETA","webCustomer":"ana","webCartId":"%s",
                                 "lines":[{"articleId":"%s","quantity":2}]}
                                """.formatted(cartId, camisa)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.webCustomer").value("ana"));

        verify(monolith).abandonCart(eq(cartId), anyString());
    }
}
