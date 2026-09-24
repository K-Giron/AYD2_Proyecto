package com.megastore.sgologic.audit;

import com.megastore.sgologic.audit.api.AuditEntryRequest;
import com.megastore.sgologic.audit.api.AuditRecorder;
import com.megastore.sgologic.incidents.Incident;
import com.megastore.sgologic.incidents.IncidentLineItem;
import com.megastore.sgologic.incidents.IncidentService;
import com.megastore.sgologic.incidents.IncidentStatus;
import com.megastore.sgologic.incidents.IncidentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AUD-01 contra PostgreSQL y RabbitMQ reales: evidencia atómica con el
 * cambio de negocio, actor desde el contexto autenticado o {@code system},
 * y registro automático al operar una incidencia (primer consumidor).
 */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class AuditIntegrationTest {

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
    PlatformTransactionManager transactionManager;

    @Autowired
    AuditRecorder auditRecorder;

    @Autowired
    AuditEventRepository repository;

    @Autowired
    IncidentService incidentService;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordsActorInstantChangeResultAndCorrelation() {
        UUID aggregateId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();

        UUID id = transactionTemplate.execute(status -> auditRecorder.record(new AuditEntryRequest(
                "Order", aggregateId, orderId, "ORDER_CONFIRMED", "CONFIRMED",
                "{\"status\":\"FISCAL_PENDING\"}", "{\"status\":\"CONFIRMED\"}",
                correlationId, null, null)));

        List<AuditEvent> events = repository.findByAggregateTypeAndAggregateIdOrderByOccurredAtAsc(
                "Order", aggregateId);
        assertThat(events).hasSize(1);
        AuditEvent event = events.get(0);
        assertThat(event.getId()).isEqualTo(id);
        assertThat(event.getActor()).isEqualTo("system");
        assertThat(event.getOrderId()).isEqualTo(orderId);
        assertThat(event.getAction()).isEqualTo("ORDER_CONFIRMED");
        assertThat(event.getResult()).isEqualTo("CONFIRMED");
        assertThat(event.getBeforeState()).contains("FISCAL_PENDING");
        assertThat(event.getAfterState()).contains("CONFIRMED");
        assertThat(event.getCorrelationId()).isEqualTo(correlationId);
        assertThat(event.getOccurredAt()).isNotNull();
    }

    @Test
    void takesActorFromSecurityContextWhenAuthenticated() {
        UUID aggregateId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "finanzas@megastore.com", null, List.of(new SimpleGrantedAuthority("ROLE_FINANCE"))));

        transactionTemplate.execute(status -> auditRecorder.record(AuditEntryRequest.of(
                "FiscalDocument", aggregateId, UUID.randomUUID(), "FISCAL_APPROVED", "APPROVED",
                UUID.randomUUID())));

        assertThat(repository.findByAggregateTypeAndAggregateIdOrderByOccurredAtAsc("FiscalDocument", aggregateId))
                .singleElement()
                .satisfies(event -> assertThat(event.getActor()).isEqualTo("finanzas@megastore.com"));
    }

    @Test
    void recordWithoutActiveTransactionIsRejected() {
        assertThatThrownBy(() -> auditRecorder.record(AuditEntryRequest.of(
                "Order", UUID.randomUUID(), UUID.randomUUID(), "ORDER_CONFIRMED", "CONFIRMED",
                UUID.randomUUID())))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void incidentOperationsLeaveAuditEvidence() {
        UUID orderId = UUID.randomUUID();
        Incident incident = incidentService.register(orderId, IncidentType.RETURN, "Producto dañado", false,
                List.of(new IncidentLineItem("SKU-9", 1)), UUID.randomUUID());

        assertThat(repository.findByAggregateTypeAndAggregateIdOrderByOccurredAtAsc("Incident", incident.getId()))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.getAction()).isEqualTo("INCIDENT_REGISTERED");
                    assertThat(event.getResult()).isEqualTo("REGISTERED");
                    assertThat(event.getOrderId()).isEqualTo(orderId);
                    assertThat(event.getActor()).isEqualTo("system");
                });

        incidentService.resolve(incident.getId(), IncidentStatus.REFUNDED, new java.math.BigDecimal("50.00"));

        assertThat(repository.findByAggregateTypeAndAggregateIdOrderByOccurredAtAsc("Incident", incident.getId()))
                .hasSize(2)
                .last()
                .satisfies(event -> {
                    assertThat(event.getAction()).isEqualTo("INCIDENT_RESOLVED");
                    assertThat(event.getResult()).isEqualTo("REFUNDED");
                    assertThat(event.getBeforeState()).contains("REGISTERED");
                    assertThat(event.getAfterState()).contains("REFUNDED");
                });
    }
}
