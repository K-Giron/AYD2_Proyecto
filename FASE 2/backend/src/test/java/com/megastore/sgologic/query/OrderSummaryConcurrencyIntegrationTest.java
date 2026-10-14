package com.megastore.sgologic.query;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * QRY-01 contra PostgreSQL real: la orden, el pago y el despacho llegan a la
 * vez con la primera noticia de una orden (cada uno por su cola). Antes uno
 * fallaba por clave duplicada y su dato se perdía; ahora los tres quedan.
 */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class OrderSummaryConcurrencyIntegrationTest {

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
    }

    @Autowired
    OrderSummaryQueryService service;

    @Test
    void firstFactsArrivingTogetherAreAllKept() throws Exception {
        for (int round = 0; round < 10; round++) {
            UUID orderId = UUID.randomUUID();
            Instant now = Instant.now();
            CountDownLatch start = new CountDownLatch(1);
            ExecutorService pool = Executors.newFixedThreadPool(3);
            List<Future<?>> writes = List.of(
                    pool.submit(() -> { start.await(); return service.upsert(orderId, ProjectionSource.ORDER, "CONFIRMED", now); }),
                    pool.submit(() -> { start.await(); return service.upsert(orderId, ProjectionSource.PAYMENT, "APPROVED", now); }),
                    pool.submit(() -> { start.await(); return service.upsert(orderId, ProjectionSource.DISPATCH, "PREPARING", now); }));
            start.countDown();
            for (Future<?> write : writes) {
                write.get();
            }
            pool.shutdown();

            OrderSummaryProjection projection = service.findByOrderId(orderId);
            assertThat(projection.getOrderStatus()).isEqualTo("CONFIRMED");
            assertThat(projection.getPaymentStatus()).isEqualTo("APPROVED");
            assertThat(projection.getDispatchStatus()).isEqualTo("PREPARING");
        }
    }
}
