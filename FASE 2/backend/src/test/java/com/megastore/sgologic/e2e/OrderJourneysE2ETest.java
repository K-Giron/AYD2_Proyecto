package com.megastore.sgologic.e2e;

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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ORD-05 / QA-04: cinco recorridos de usuario desde autenticación y carrito
 * hasta un resultado terminal observable, sobre PostgreSQL y RabbitMQ reales.
 */
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = {
        "sgo.security.enabled=true",
        "sgo.simulators.payklik.mode=SUCCESS",
        "sgo.simulators.sat.mode=SUCCESS",
        "sgo.simulators.logiexpress.mode=SUCCESS",
        "sgo.simulators.notifications.mode=SUCCESS"
})
class OrderJourneysE2ETest {

    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111102");
    private static final Duration AWAIT_TIMEOUT = Duration.ofSeconds(15);

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17.11-alpine3.23"))
            .withDatabaseName("sgo_logic_e2e")
            .withUsername("sgo_e2e")
            .withPassword("e2e_only_password");

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

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void customerLogsInBuysProductAndSagaStartsDispatch() throws Exception {
        String client = login("cliente@megastore.com", "Cliente123");
        Purchase purchase = purchase(client, "WEB", "Zona 10, Guatemala");

        assertThat(purchase.saga().get("sagaStatus").asText()).isEqualTo("COMPLETED");
        assertThat(purchase.saga().get("orderStatus").asText()).isEqualTo("PREPARING");
        assertThat(purchase.saga().get("shipmentId").asText()).isNotBlank();
        assertThat(getJson("/api/v1/inventory/reservations/" + purchase.reservationId(), client)
                .get("status").asText()).isEqualTo("CONFIRMED");
    }

    @Test
    void customerRecoversCartOnSecondChannelAndRepeatedSagaDoesNotDuplicate() throws Exception {
        String client = login("cliente@megastore.com", "Cliente123");
        UUID customerId = UUID.randomUUID();
        JsonNode webCart = openCart(client, customerId, "WEB", key("cart-web"));
        addProduct(client, webCart.get("id").asText(), 1);
        JsonNode mobileCart = openCart(client, customerId, "MOBILE", key("cart-mobile"));
        assertThat(mobileCart.get("id").asText()).isEqualTo(webCart.get("id").asText());
        assertThat(mobileCart.get("lastChannel").asText()).isEqualTo("MOBILE");

        JsonNode order = checkout(client, webCart.get("id").asText(), key("checkout"));
        String sagaKey = key("saga-recovery");
        JsonNode completed = awaitSaga(client, order.get("id").asText(), sagaKey, "Zona 4, Mixco");
        JsonNode repeated = runSaga(client, order.get("id").asText(), sagaKey, "Zona 4, Mixco");

        assertThat(repeated.get("shipmentId").asText()).isEqualTo(completed.get("shipmentId").asText());
        JsonNode shipments = getJson("/api/v1/shipments?orderId=" + order.get("id").asText(), client);
        assertThat(shipments.size()).isEqualTo(1);
    }

    @Test
    void customerCancelsBeforePaymentAndReservationIsReleasedIdempotently() throws Exception {
        String client = login("cliente@megastore.com", "Cliente123");
        UUID customerId = UUID.randomUUID();
        JsonNode cart = openCart(client, customerId, "WEB", key("cart-cancel"));
        addProduct(client, cart.get("id").asText(), 2);
        JsonNode order = checkout(client, cart.get("id").asText(), key("checkout-cancel"));
        JsonNode reserved = postJson("/api/v1/orders/" + order.get("id").asText()
                        + "/inventory-reservations", client, null,
                Map.of("Idempotency-Key", key("reserve-cancel")));
        String reservationId = reserved.get("reservations").get(0).get("reservationId").asText();

        JsonNode cancelled = postJson("/api/v1/orders/" + order.get("id").asText()
                + "/cancel?reason=Cliente+cambio+de+opinion", client, null, Map.of());
        JsonNode repeated = postJson("/api/v1/orders/" + order.get("id").asText()
                + "/cancel?reason=Solicitud+repetida", client, null, Map.of());

        assertThat(cancelled.get("status").asText()).isEqualTo("CANCELLED");
        assertThat(repeated.get("status").asText()).isEqualTo("CANCELLED");
        assertThat(getJson("/api/v1/inventory/reservations/" + reservationId, client)
                .get("status").asText()).isEqualTo("RELEASED");
    }

    @Test
    void customerBuysAndLogisticsCompletesDelivery() throws Exception {
        String client = login("cliente@megastore.com", "Cliente123");
        String staff = login("staff@megastore.com", "Staff123");
        Purchase purchase = purchase(client, "MOBILE", "Zona 1, Guatemala");

        moveShipmentAndOrder(staff, purchase.orderId(), purchase.shipmentId(), "IN_TRANSIT");
        moveShipmentAndOrder(staff, purchase.orderId(), purchase.shipmentId(), "DELIVERED");

        assertThat(getJson("/api/v1/orders/" + purchase.orderId(), client).get("status").asText())
                .isEqualTo("DELIVERED");
        assertThat(getJson("/api/v1/shipments/" + purchase.shipmentId(), client).get("status").asText())
                .isEqualTo("DELIVERED");
    }

    @Test
    void customerBuysReceivesOrderRequestsReturnAndStaffRefunds() throws Exception {
        String client = login("cliente@megastore.com", "Cliente123");
        String staff = login("staff@megastore.com", "Staff123");
        Purchase purchase = purchase(client, "STORE", "Recoger en tienda 05");
        moveShipmentAndOrder(staff, purchase.orderId(), purchase.shipmentId(), "IN_TRANSIT");
        moveShipmentAndOrder(staff, purchase.orderId(), purchase.shipmentId(), "DELIVERED");

        JsonNode incident = postJson("/api/v1/incidents", client, Map.of(
                "orderId", purchase.orderId(), "type", "RETURN",
                "reason", "Talla incorrecta", "partial", false,
                "items", new Object[]{Map.of("sku", "SKU-PANTALON-NEGRO", "quantity", 1)}), Map.of());
        JsonNode refunded = patchJson("/api/v1/incidents/" + incident.get("id").asText() + "/resolution",
                staff, Map.of("resolution", "REFUNDED", "refundAmount", new BigDecimal("399.00")));

        assertThat(refunded.get("status").asText()).isEqualTo("REFUNDED");
        assertThat(refunded.get("refundAmount").decimalValue()).isEqualByComparingTo("399.00");
        assertThat(getJson("/api/v1/incidents?orderId=" + purchase.orderId(), client).size()).isEqualTo(1);
    }

    private Purchase purchase(String token, String channel, String destination) throws Exception {
        JsonNode cart = openCart(token, UUID.randomUUID(), channel, key("cart"));
        addProduct(token, cart.get("id").asText(), 1);
        JsonNode order = checkout(token, cart.get("id").asText(), key("checkout"));
        String sagaKey = key("saga");
        JsonNode saga = awaitSaga(token, order.get("id").asText(), sagaKey, destination);
        JsonNode persistedOrder = getJson("/api/v1/orders/" + order.get("id").asText(), token);
        assertThat(persistedOrder.get("status").asText()).isEqualTo("PREPARING");
        JsonNode reservations = getJson("/api/v1/inventory/reservations/"
                + saga.get("reservationId").asText(), token);
        return new Purchase(order.get("id").asText(), saga.get("shipmentId").asText(),
                reservations.get("reservationId").asText(), saga);
    }

    private JsonNode awaitSaga(String token, String orderId, String sagaKey, String destination) throws Exception {
        long deadline = System.nanoTime() + AWAIT_TIMEOUT.toNanos();
        JsonNode result;
        do {
            result = runSaga(token, orderId, sagaKey, destination);
            if (!"WAITING_FISCAL".equals(result.get("sagaStatus").asText())) return result;
            Thread.sleep(150);
        } while (System.nanoTime() < deadline);
        throw new AssertionError("La Saga no salió de WAITING_FISCAL: " + result);
    }

    private JsonNode runSaga(String token, String orderId, String sagaKey, String destination) throws Exception {
        return postJson("/api/v1/orders/" + orderId + "/saga", token, Map.of(
                "paymentMethod", "CARD", "currency", "GTQ", "paymentReference", "tok_e2e_4242",
                "taxIdentifier", "CF-E2E", "customerName", "Cliente E2E", "destination", destination),
                Map.of("Idempotency-Key", sagaKey, "X-Actor", "customer:e2e"));
    }

    private void moveShipmentAndOrder(String token, String orderId, String shipmentId, String state) throws Exception {
        postJson("/api/v1/shipments/" + shipmentId + "/tracking-events", token,
                Map.of("status", state), Map.of());
        patchJson("/api/v1/orders/" + orderId + "/status", token,
                Map.of("status", state, "actor", "LOGISTICS:e2e", "reason", "E2E " + state));
    }

    private JsonNode openCart(String token, UUID customerId, String channel, String key) throws Exception {
        return postJson("/api/v1/carts", token, Map.of("customerId", customerId, "channel", channel),
                Map.of("Idempotency-Key", key));
    }

    private void addProduct(String token, String cartId, int quantity) throws Exception {
        String content = mapper.writeValueAsString(Map.of(
                "productName", "Pantalón de lona negro", "unitPrice", new BigDecimal("399.00"),
                "quantity", quantity));
        mvc.perform(put("/api/v1/carts/" + cartId + "/items/" + PRODUCT_ID)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(content))
                .andExpect(status().isOk());
    }

    private JsonNode checkout(String token, String cartId, String key) throws Exception {
        return postJson("/api/v1/carts/" + cartId + "/checkout", token, null,
                Map.of("Idempotency-Key", key, "X-Actor", "customer:e2e"));
    }

    private String login(String username, String password) throws Exception {
        return postJson("/api/v1/auth/login", null, Map.of("username", username, "password", password), Map.of())
                .get("accessToken").asText();
    }

    private JsonNode getJson(String path, String token) throws Exception {
        String content = mvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(content);
    }

    private JsonNode postJson(String path, String token, Object body, Map<String, String> headers) throws Exception {
        var request = post(path).contentType(MediaType.APPLICATION_JSON);
        if (token != null) request.header("Authorization", "Bearer " + token);
        headers.forEach(request::header);
        if (body != null) request.content(mapper.writeValueAsString(body));
        String content = mvc.perform(request).andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(content);
    }

    private JsonNode patchJson(String path, String token, Object body) throws Exception {
        String content = mvc.perform(patch(path).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(content);
    }

    private static String key(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private record Purchase(String orderId, String shipmentId, String reservationId, JsonNode saga) {
    }
}
