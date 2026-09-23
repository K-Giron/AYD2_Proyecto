package com.megastore.sgologic.audit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AUD-02 contra PostgreSQL real: consulta por orden y por agregado, registro
 * del acceso, búsqueda fallida con alerta, exportación CSV y la medida de
 * EaC-02 (< 1 s) sobre el conjunto de datos declarado.
 */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class AuditQueryIntegrationTest {

    /** Conjunto declarado: 10 000 eventos totales, 1 000 de una misma orden. */
    private static final int TOTAL_EVENTS = 10_000;
    private static final int ORDER_EVENTS = 1_000;

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
    AuditQueryService queryService;

    @Autowired
    AuditEventRepository repository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void findsEvidenceByOrderAndRecordsAccess() {
        UUID orderId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        seed(orderId, "Order", aggregateId, "ORDER_CONFIRMED", Instant.parse("2026-09-23T10:00:00Z"));
        seed(orderId, "Order", aggregateId, "INVOICE_APPROVED", Instant.parse("2026-09-23T10:00:05Z"));

        List<AuditEvent> events = queryService.findByOrder(orderId, UUID.randomUUID());

        assertThat(events).hasSize(2);
        assertThat(events.get(0).getAction()).isEqualTo("ORDER_CONFIRMED");
        assertThat(events.get(1).getAction()).isEqualTo("INVOICE_APPROVED");
        assertThat(repository.findByAggregateTypeAndAggregateIdOrderByOccurredAtAsc("AuditEvidence", orderId))
                .singleElement()
                .satisfies(access -> {
                    assertThat(access.getAction()).isEqualTo("AUDIT_EVIDENCE_ACCESSED");
                    assertThat(access.getActor()).isEqualTo("system");
                    assertThat(access.getResult()).isEqualTo("OK");
                });
    }

    @Test
    void findsEvidenceByAggregate() {
        UUID orderId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        seed(orderId, "PaymentAttempt", aggregateId, "PAYMENT_INDETERMINATE", Instant.parse("2026-09-23T11:00:00Z"));

        List<AuditEvent> events = queryService.findByAggregate("PaymentAttempt", aggregateId, UUID.randomUUID());

        assertThat(events).singleElement()
                .satisfies(event -> assertThat(event.getAction()).isEqualTo("PAYMENT_INDETERMINATE"));
    }

    @Test
    void missingEvidenceIsRegisteredAsFailedSearch() {
        UUID orderId = UUID.randomUUID();

        assertThatThrownBy(() -> queryService.findByOrder(orderId, UUID.randomUUID()))
                .isInstanceOf(AuditEvidenceNotFoundException.class);

        assertThat(repository.findByAggregateTypeAndAggregateIdOrderByOccurredAtAsc("AuditEvidence", orderId))
                .singleElement()
                .satisfies(failed -> {
                    assertThat(failed.getAction()).isEqualTo("AUDIT_SEARCH_FAILED");
                    assertThat(failed.getResult()).isEqualTo("NOT_FOUND");
                });
    }

    @Test
    void exportsEvidenceAsCsvAndRecordsExport() {
        UUID orderId = UUID.randomUUID();
        seed(orderId, "Shipment", UUID.randomUUID(), "SHIPMENT_DISPATCHED", Instant.parse("2026-09-23T12:00:00Z"));

        String csv = queryService.exportOrderEvidence(orderId, UUID.randomUUID());

        assertThat(csv).contains("eventId,occurredAt,actor,action,result");
        assertThat(csv).contains("\"SHIPMENT_DISPATCHED\"");
        assertThat(repository.findByAggregateTypeAndAggregateIdOrderByOccurredAtAsc("AuditEvidence", orderId))
                .singleElement()
                .satisfies(export -> assertThat(export.getAction()).isEqualTo("AUDIT_EVIDENCE_EXPORTED"));
    }

    @Test
    void queryStaysUnderOneSecondWithDeclaredDataset() {
        UUID orderId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        seedBatch(orderId, aggregateId, ORDER_EVENTS, 0);
        UUID otherOrder = UUID.randomUUID();
        seedBatch(otherOrder, UUID.randomUUID(), TOTAL_EVENTS - ORDER_EVENTS, ORDER_EVENTS);

        // Calentamiento sobre otro agregado pequeño.
        queryService.findByOrder(otherOrder, UUID.randomUUID());

        long start = System.nanoTime();
        List<AuditEvent> events = queryService.findByOrder(orderId, UUID.randomUUID());
        long elapsedMillis = Duration.ofNanos(System.nanoTime() - start).toMillis();

        assertThat(events).hasSize(ORDER_EVENTS);
        assertThat(elapsedMillis)
                .as("EaC-02: consulta de evidencia < 1 s sobre %d eventos (%d de la orden)",
                        TOTAL_EVENTS, ORDER_EVENTS)
                .isLessThan(1_000L);
    }

    private void seed(UUID orderId, String aggregateType, UUID aggregateId, String action, Instant occurredAt) {
        jdbcTemplate.update("INSERT INTO audit_events (id, event_id, aggregate_type, aggregate_id, order_id, "
                        + "actor, action, result, before_state, after_state, correlation_id, causation_id, occurred_at) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                UUID.randomUUID(), UUID.randomUUID(), aggregateType, aggregateId, orderId, "system", action,
                "OK", null, null, UUID.randomUUID(), null, Timestamp.from(occurredAt));
    }

    private void seedBatch(UUID orderId, UUID aggregateId, int count, int sequenceOffset) {
        Instant base = Instant.parse("2026-09-23T10:00:00Z");
        jdbcTemplate.batchUpdate("INSERT INTO audit_events (id, event_id, aggregate_type, aggregate_id, order_id, "
                        + "actor, action, result, before_state, after_state, correlation_id, causation_id, occurred_at) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setObject(1, UUID.randomUUID());
                        ps.setObject(2, UUID.randomUUID());
                        ps.setString(3, "Order");
                        ps.setObject(4, aggregateId);
                        ps.setObject(5, orderId);
                        ps.setString(6, "system");
                        ps.setString(7, "ORDER_STATE_CHANGED");
                        ps.setString(8, "OK");
                        ps.setString(9, null);
                        ps.setString(10, null);
                        ps.setObject(11, UUID.randomUUID());
                        ps.setObject(12, null);
                        ps.setObject(13, base.plusSeconds(sequenceOffset + i).atOffset(ZoneOffset.UTC));
                    }

                    @Override
                    public int getBatchSize() {
                        return count;
                    }
                });
    }
}
