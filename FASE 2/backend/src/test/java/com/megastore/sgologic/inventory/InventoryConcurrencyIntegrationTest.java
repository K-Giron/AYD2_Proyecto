package com.megastore.sgologic.inventory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * INV-05: prueba de concurrencia y consistencia contra infraestructura
 * real (PostgreSQL vía Testcontainers, aplicación completa por HTTP), no
 * contra dobles. Verifica la garantía central de CDU-02 E1: "Dos
 * solicitudes intentan reservar la última unidad disponible casi al mismo
 * tiempo: el sistema debe garantizar que solo una reserva se confirme".
 *
 * <p>Las migraciones V1/V2 se ejecutan igual que en cualquier ambiente:
 * {@code ST-01} queda sembrada con exactamente 1 unidad del producto de
 * demostración {@code 11111111-1111-1111-1111-111111111101} (ver
 * {@code V2__inventory_seed_locations_and_stock.sql}), la misma
 * combinación usada para validar esto manualmente en Docker antes de
 * escribir esta prueba.</p>
 */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InventoryConcurrencyIntegrationTest {

    private static final UUID SEED_PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111101");
    private static final String SEED_SINGLE_UNIT_LOCATION_CODE = "ST-01";

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
        // La expiración por defecto es 15 min; nada en esta prueba debe
        // expirar mientras corre, así que se deja el valor por defecto.
    }

    @Autowired
    DataSource dataSource;

    @LocalServerPort
    int serverPort;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * CDU-02 E1, con margen: 10 solicitudes verdaderamente concurrentes
     * (todas liberadas al mismo tiempo mediante {@link CountDownLatch})
     * compiten por la única unidad sembrada en {@code ST-01}. Exactamente
     * una debe ganar (201); el resto debe recibir 409 sin excepción.
     */
    @Test
    void exactlyOneOfManyConcurrentRequestsWinsTheLastUnit() throws Exception {
        UUID locationId = findSeedLocationId(SEED_SINGLE_UNIT_LOCATION_CODE);
        assertAvailableQuantity(SEED_PRODUCT_ID, locationId, 1);

        int concurrentRequests = 10;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        CountDownLatch readyLatch = new CountDownLatch(concurrentRequests);
        CountDownLatch startLatch = new CountDownLatch(1);

        try {
            List<Future<Integer>> futures = IntStream.range(0, concurrentRequests)
                    .mapToObj(i -> executor.submit(() -> {
                        readyLatch.countDown();
                        startLatch.await();
                        return reserve(UUID.randomUUID(), SEED_PRODUCT_ID, locationId, 1,
                                "inv-05-race-" + i);
                    }))
                    .toList();

            readyLatch.await(10, TimeUnit.SECONDS);
            startLatch.countDown();

            List<Integer> statusCodes = futures.stream()
                    .map(this::getQuietly)
                    .toList();

            long successCount = statusCodes.stream().filter(code -> code == 201).count();
            long conflictCount = statusCodes.stream().filter(code -> code == 409).count();

            assertThat(successCount)
                    .as("Exactamente una de %d solicitudes concurrentes debe reservar la última unidad", concurrentRequests)
                    .isEqualTo(1);
            assertThat(conflictCount).isEqualTo(concurrentRequests - 1);
            assertThat(statusCodes).allMatch(code -> code == 201 || code == 409);
        } finally {
            executor.shutdownNow();
        }

        assertAvailableQuantity(SEED_PRODUCT_ID, locationId, 0);
    }

    /**
     * Control de sanidad: sin contención, una reserva por debajo del
     * disponible siempre debe tener éxito. Da contexto a la prueba
     * anterior (que solo se pone interesante bajo contención real).
     */
    @Test
    void reservationSucceedsWhenThereIsNoContention() throws Exception {
        UUID locationId = findSeedLocationId("CD-02");
        int status = reserve(UUID.randomUUID(), UUID.fromString("11111111-1111-1111-1111-111111111103"),
                locationId, 1, "inv-05-no-contention");
        assertThat(status).isEqualTo(201);
    }

    private int reserve(UUID orderId, UUID productId, UUID locationId, int quantity, String idempotencyKey)
            throws Exception {
        String body = """
                {"orderId":"%s","productId":"%s","locationId":"%s","quantity":%d}
                """.formatted(orderId, productId, locationId, quantity);
        HttpRequest request = HttpRequest.newBuilder(baseUri("/api/v1/inventory/reservations"))
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", idempotencyKey)
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode();
    }

    private void assertAvailableQuantity(UUID productId, UUID locationId, int expectedAvailable) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                        baseUri("/api/v1/inventory/stock?productId=" + productId + "&locationId=" + locationId))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"quantityAvailable\":" + expectedAvailable);
    }

    private UUID findSeedLocationId(String code) throws Exception {
        try (var connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(
                    "SELECT id FROM inventory_locations WHERE code = '" + code + "'");
            assertThat(resultSet.next()).as("Ubicación semilla %s debe existir (V2)", code).isTrue();
            return UUID.fromString(resultSet.getString("id"));
        }
    }

    private URI baseUri(String path) {
        return URI.create("http://localhost:" + serverPort + path);
    }

    private Integer getQuietly(Future<Integer> future) {
        try {
            return future.get(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("Una solicitud concurrente de la prueba falló inesperadamente", e);
        }
    }
}
