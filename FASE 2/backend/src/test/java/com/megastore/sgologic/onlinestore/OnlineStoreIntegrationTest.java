package com.megastore.sgologic.onlinestore;

import com.megastore.sgologic.inventory.InventoryReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tienda online de punta a punta sobre PostgreSQL y RabbitMQ reales, con el
 * almacén central simulado: recepción por folio que ingresa al inventario
 * (CD-WEB), publicación con imagen, catálogo público con disponibilidad
 * real, compra web cuya reserva cae en CD-WEB y reserva concurrente de la
 * última unidad sin doble compromiso.
 */
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = {
        "sgo.security.enabled=true",
        "sgo.integrations.online-store.sync-enabled=false",
        "sgo.simulators.payklik.mode=SUCCESS",
        "sgo.simulators.sat.mode=SUCCESS",
        "sgo.simulators.logiexpress.mode=SUCCESS",
        "sgo.simulators.notifications.mode=SUCCESS"
})
class OnlineStoreIntegrationTest {

    private static final Duration AWAIT_TIMEOUT = Duration.ofSeconds(15);
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0, 0, 13};

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17.11-alpine3.23"))
            .withDatabaseName("sgo_logic_online")
            .withUsername("sgo_online")
            .withPassword("online_only_password");

    @Container
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:4.3.5-management-alpine"));

    @DynamicPropertySource
    static void infrastructure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);
    }

    @MockitoBean
    CentralReceiptsClient central;

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired InventoryReservationService reservationService;

    private void centralSends(String folio, String shipment, UUID articleId, String sku, String name, int quantity) {
        when(central.receiptsSince(any())).thenReturn(List.of(new IncomingReceipt(folio, shipment, "CENTRAL",
                articleId, sku, name, "Descripción del central", new BigDecimal("250.00"), quantity,
                Instant.now())));
    }

    @Test
    void receivedByFolioPublishedWithImageAndSoldFromTheOnlineStoreStock() throws Exception {
        UUID articleId = UUID.randomUUID();
        String admin = login("tienda-online@megastore.com", "TiendaOnline123");
        String client = login("cliente@megastore.com", "Cliente123");
        centralSends("POS-ENT-000101", "SAL-000101", articleId, "WEB-TEN-01", "Tenis running", 2);

        // Folio + Tab: se carga lo que trae, en tránsito; todavía no hay nada que vender.
        mvc.perform(get("/api/v1/online-store/receiving/sal-000101").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EN_TRANSITO"))
                .andExpect(jsonPath("$.units").value(2));
        mvc.perform(get("/api/v1/online-catalog")).andExpect(jsonPath("$.length()").value(0));

        mvc.perform(post("/api/v1/online-store/receiving/SAL-000101/confirm").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECIBIDO"))
                .andExpect(jsonPath("$.receivedBy").value("tienda-online@megastore.com"));
        mvc.perform(post("/api/v1/online-store/receiving/SAL-000101/confirm").header("Authorization", bearer(admin)))
                .andExpect(status().isConflict());

        // Entró al inventario real de la tienda online, sin publicar.
        mvc.perform(get("/api/v1/inventory/stock").param("productId", articleId.toString()))
                .andExpect(jsonPath("$[0].locationCode").value("CD-WEB"))
                .andExpect(jsonPath("$[0].quantityOnHand").value(2));
        mvc.perform(get("/api/v1/online-store/products").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$[0].published").value(false))
                .andExpect(jsonPath("$[0].available").value(2));
        mvc.perform(get("/api/v1/online-catalog")).andExpect(jsonPath("$.length()").value(0));

        // Publicar exige imagen.
        mvc.perform(post("/api/v1/online-store/products/" + articleId + "/publish").header("Authorization", bearer(admin)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IMAGE_REQUIRED"));
        mvc.perform(put("/api/v1/online-store/products/" + articleId).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Tenis running azul\",\"description\":\"Suela amortiguada\",\"category\":\"Calzado\",\"price\":299.90}"))
                .andExpect(status().isOk());
        mvc.perform(put("/api/v1/online-store/products/" + articleId + "/image").header("Authorization", bearer(admin))
                        .contentType(MediaType.IMAGE_PNG).content(PNG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.image").exists());
        mvc.perform(post("/api/v1/online-store/products/" + articleId + "/publish").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.published").value(true));

        JsonNode catalog = mapper.readTree(mvc.perform(get("/api/v1/online-catalog"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(catalog).hasSize(1);
        assertThat(catalog.get(0).get("name").asText()).isEqualTo("Tenis running azul");
        assertThat(catalog.get(0).get("category").asText()).isEqualTo("Calzado");
        assertThat(catalog.get(0).get("available").asInt()).isEqualTo(2);
        mvc.perform(get(catalog.get(0).get("image").asText()))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().bytes(PNG));

        // El cliente no administra la tienda online.
        mvc.perform(get("/api/v1/online-store/products").header("Authorization", bearer(client)))
                .andExpect(status().isForbidden());

        // Compra web: la reserva y la confirmación caen en CD-WEB.
        JsonNode cart = postJson("/api/v1/carts", client, Map.of("customerId", UUID.randomUUID(), "channel", "WEB"),
                Map.of("Idempotency-Key", "cart-" + UUID.randomUUID()));
        mvc.perform(put("/api/v1/carts/" + cart.get("id").asText() + "/items/" + articleId)
                        .header("Authorization", bearer(client)).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("productName", "Tenis running azul",
                                "unitPrice", new BigDecimal("299.90"), "quantity", 1))))
                .andExpect(status().isOk());
        JsonNode order = postJson("/api/v1/carts/" + cart.get("id").asText() + "/checkout", client, null,
                Map.of("Idempotency-Key", "checkout-" + UUID.randomUUID(), "X-Actor", "customer:online-e2e"));
        JsonNode saga = awaitSaga(client, order.get("id").asText(), "saga-" + UUID.randomUUID());
        assertThat(saga.get("orderStatus").asText()).isEqualTo("PREPARING");
        JsonNode reservation = getJson("/api/v1/inventory/reservations/" + saga.get("reservationId").asText(), client);
        assertThat(reservation.get("status").asText()).isEqualTo("CONFIRMED");
        // El despacho sale del almacén de la tienda online, y el cliente puede ver su factura.
        JsonNode shipments = getJson("/api/v1/shipments?orderId=" + order.get("id").asText(), client);
        assertThat(shipments.get(0).get("originCode").asText()).isEqualTo("CD-WEB");
        JsonNode invoice = getJson("/api/v1/invoices/orders/" + order.get("id").asText(), client);
        assertThat(invoice.get("cui").asText()).isNotBlank();
        assertThat(invoice.get("status").asText()).isEqualTo("APPROVED");
        assertThat(invoice.get("xml").asText()).contains("<CUI>");
        assertThat(invoice.has("paymentAttemptId")).isFalse();

        mvc.perform(get("/api/v1/online-store/products").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$[0].onHand").value(1))
                .andExpect(jsonPath("$[0].reserved").value(0))
                .andExpect(jsonPath("$[0].available").value(1));
        mvc.perform(get("/api/v1/online-catalog")).andExpect(jsonPath("$[0].available").value(1));

        // Kárdex: la entrada por folio y la reserva de la venta, ya confirmada.
        mvc.perform(get("/api/v1/online-store/movements").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$[?(@.kind == 'ENTRADA' && @.productId == '" + articleId + "')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.kind == 'RESERVA' && @.status == 'CONFIRMED' && @.productId == '"
                        + articleId + "')]").isNotEmpty());
        // Puntos de recogida activos, públicos para el checkout.
        mvc.perform(get("/api/v1/online-catalog/pickup-points"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code").value(org.hamcrest.Matchers.contains("ALM-CENTRAL", "POS-01")));
    }

    @Test
    void transfersLeaveTheOnlineWarehouseByFolioAndRejectedOnesComeBack() throws Exception {
        UUID articleId = UUID.randomUUID();
        String admin = login("tienda-online@megastore.com", "TiendaOnline123");
        centralSends("POS-ENT-000301", "SAL-000301", articleId, "WEB-TRS-01", "Sandalias", 3);
        mvc.perform(post("/api/v1/online-store/receiving/SAL-000301/confirm").header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
        // Otro envío queda en tránsito: se ve en el inventario pero no se puede trasladar ni vender.
        when(central.receiptsSince(any())).thenReturn(List.of(new IncomingReceipt("POS-ENT-000302", "SAL-000302",
                "CENTRAL", articleId, "WEB-TRS-01", "Sandalias", null, new BigDecimal("90.00"), 4, Instant.now())));
        mvc.perform(post("/api/v1/online-store/sync").header("Authorization", bearer(admin))).andExpect(status().isOk());

        // Aceptado por el central: sale de CD-WEB.
        when(central.registerTransfer(any())).thenReturn(
                new CentralReceiptsClient.TransferReceipt("TRA-000009", "TOL-000001", "ENT-000044"));
        mvc.perform(post("/api/v1/online-store/transfers").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + articleId + "\",\"quantity\":1,\"destination\":\"central\",\"reason\":\"Talla equivocada\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folio").value(org.hamcrest.Matchers.startsWith("TOL-")))
                .andExpect(jsonPath("$.destination").value("CENTRAL"))
                .andExpect(jsonPath("$.syncStatus").value("ENVIADA"))
                .andExpect(jsonPath("$.destinationFolio").value("ENT-000044"));

        // Rechazado por el central: vuelve a CD-WEB.
        when(central.registerTransfer(any())).thenThrow(new OnlineStoreException(
                org.springframework.http.HttpStatus.CONFLICT, "TRANSFER_REJECTED", "No alcanza la existencia."));
        mvc.perform(post("/api/v1/online-store/transfers").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + articleId + "\",\"quantity\":1,\"destination\":\"POS-01\"}"))
                .andExpect(jsonPath("$.syncStatus").value("RECHAZADO"));

        // Más de lo disponible: no sale nada.
        mvc.perform(post("/api/v1/online-store/transfers").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + articleId + "\",\"quantity\":5,\"destination\":\"CENTRAL\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));

        mvc.perform(get("/api/v1/online-store/inventory").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$[?(@.sku == 'WEB-TRS-01')].onHand").value(2))
                .andExpect(jsonPath("$[?(@.sku == 'WEB-TRS-01')].inTransit").value(4));
        mvc.perform(get("/api/v1/online-store/sync-log").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$[?(@.kind == 'TRASLADO' && @.result == 'OK')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.kind == 'TRASLADO' && @.result == 'ERROR')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.kind == 'RECEPCION' && @.reference == 'SAL-000301')]").isNotEmpty());
    }

    @Test
    void twoConcurrentBuyersForTheLastOnlineUnitProduceExactlyOneReservation() throws Exception {
        UUID articleId = UUID.randomUUID();
        String admin = login("tienda-online@megastore.com", "TiendaOnline123");
        centralSends("POS-ENT-000201", "SAL-000201", articleId, "WEB-LAST-01", "Última unidad", 1);
        mvc.perform(post("/api/v1/online-store/receiving/SAL-000201/confirm").header("Authorization", bearer(admin)))
                .andExpect(status().isOk());

        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> buyer = () -> {
            start.await();
            try {
                reservationService.reserveAtBestOrigin(UUID.randomUUID(), articleId, 1, UUID.randomUUID(),
                        "concurrent-" + UUID.randomUUID());
                return true;
            } catch (RuntimeException noStock) {
                return false;
            }
        };
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = pool.submit(buyer);
            Future<Boolean> second = pool.submit(buyer);
            start.countDown();
            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(true, false);
        } finally {
            pool.shutdownNow();
        }
        mvc.perform(get("/api/v1/inventory/stock").param("productId", articleId.toString()))
                .andExpect(jsonPath("$[0].quantityReserved").value(1))
                .andExpect(jsonPath("$[0].quantityAvailable").value(0));
    }

    private JsonNode awaitSaga(String token, String orderId, String sagaKey) throws Exception {
        long deadline = System.nanoTime() + AWAIT_TIMEOUT.toNanos();
        JsonNode result;
        do {
            result = postJson("/api/v1/orders/" + orderId + "/saga", token, Map.of(
                    "paymentMethod", "CARD", "currency", "GTQ", "paymentReference", "tok_online_4242",
                    "taxIdentifier", "CF", "customerName", "Cliente Online", "destination", "Zona 10"),
                    Map.of("Idempotency-Key", sagaKey, "X-Actor", "customer:online-e2e"));
            if (!"WAITING_FISCAL".equals(result.get("sagaStatus").asText())) return result;
            Thread.sleep(150);
        } while (System.nanoTime() < deadline);
        throw new AssertionError("La Saga no salió de WAITING_FISCAL: " + result);
    }

    private String login(String username, String password) throws Exception {
        return postJson("/api/v1/auth/login", null, Map.of("username", username, "password", password), Map.of())
                .get("accessToken").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private JsonNode getJson(String path, String token) throws Exception {
        return mapper.readTree(mvc.perform(get(path).header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private JsonNode postJson(String path, String token, Object body, Map<String, String> headers) throws Exception {
        var request = post(path).contentType(MediaType.APPLICATION_JSON);
        if (token != null) request.header("Authorization", bearer(token));
        headers.forEach(request::header);
        if (body != null) request.content(mapper.writeValueAsString(body));
        return mapper.readTree(mvc.perform(request).andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString());
    }
}
