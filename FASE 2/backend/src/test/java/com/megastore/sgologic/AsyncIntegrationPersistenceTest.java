package com.megastore.sgologic;

import com.megastore.sgologic.fiscal.FiscalDocument;
import com.megastore.sgologic.fiscal.FiscalDocumentType;
import com.megastore.sgologic.fiscal.FiscalService;
import com.megastore.sgologic.fiscal.FiscalStatus;
import com.megastore.sgologic.integration.ErpBatch;
import com.megastore.sgologic.integration.ErpBatchStatus;
import com.megastore.sgologic.integration.ErpExportFormat;
import com.megastore.sgologic.integration.ErpIntegrationService;
import com.megastore.sgologic.integration.PosDelivery;
import com.megastore.sgologic.integration.PosDeliveryStatus;
import com.megastore.sgologic.integration.PosIntegrationService;
import com.megastore.sgologic.logistics.DispatchOrigin;
import com.megastore.sgologic.logistics.Shipment;
import com.megastore.sgologic.logistics.ShipmentService;
import com.megastore.sgologic.notifications.NotificationChannel;
import com.megastore.sgologic.notifications.NotificationEventType;
import com.megastore.sgologic.notifications.NotificationRequest;
import com.megastore.sgologic.notifications.NotificationService;
import com.megastore.sgologic.notifications.NotificationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regresión de persistencia real de los flujos asíncronos.
 *
 * <p>Los servicios de fiscal, ERP, POS, despacho y notificaciones guardan la
 * misma entidad dos veces (marca de intento y resultado). Si no se reasigna
 * la copia gestionada que devuelve {@code save}, el segundo guardado usa la
 * versión obsoleta del objeto desconectado y falla con
 * {@code ObjectOptimisticLockingFailureException}: el flujo queda atascado en
 * {@code PENDING} o {@code FISCAL_PENDING} para siempre.</p>
 *
 * <p>Las pruebas unitarias con repositorios en memoria no reproducen ese
 * comportamiento porque no existe {@code @Version} real. Esta clase usa
 * PostgreSQL de Testcontainers y los ejecutores asíncronos reales, y espera
 * el estado terminal de cada flujo.</p>
 */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
        "sgo.simulators.sat.mode=SUCCESS",
        "sgo.simulators.erp.mode=SUCCESS",
        "sgo.simulators.pos.mode=SUCCESS",
        "sgo.simulators.logiexpress.mode=SUCCESS",
        "sgo.simulators.notifications.mode=SUCCESS"
})
class AsyncIntegrationPersistenceTest {

    private static final Duration AWAIT_TIMEOUT = Duration.ofSeconds(15);

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
    FiscalService fiscalService;

    @Autowired
    ErpIntegrationService erpIntegrationService;

    @Autowired
    PosIntegrationService posIntegrationService;

    @Autowired
    ShipmentService shipmentService;

    @Autowired
    NotificationService notificationService;

    @Test
    void fiscalDocumentReachesApprovedWithoutOptimisticLockFailure() {
        UUID orderId = UUID.randomUUID();
        FiscalDocument document = fiscalService.requestDocument(
                orderId, UUID.randomUUID(), FiscalDocumentType.INVOICE, null,
                "CF-1234567", "Cliente Prueba", new BigDecimal("150.00"), "GTQ",
                UUID.randomUUID(), "it-fis-" + UUID.randomUUID());

        await(() -> fiscalService.getDocument(document.getId()).getStatus() == FiscalStatus.APPROVED,
                "documento fiscal APPROVED");

        assertThat(fiscalService.getDocument(document.getId()).getSubmissionAttempts()).isEqualTo(1);
        assertThat(fiscalService.canConfirmOrder(orderId)).isTrue();
    }

    @Test
    void posDeliveryReachesAcknowledgedWithoutOptimisticLockFailure() {
        UUID eventId = UUID.randomUUID();
        PosDelivery delivery = posIntegrationService.publish(
                eventId, "OrderConfirmed", UUID.randomUUID(), 1, "{\"orderId\":\"" + eventId + "\"}",
                UUID.randomUUID());

        await(() -> posIntegrationService.getDelivery(delivery.getId()).getStatus() == PosDeliveryStatus.ACKNOWLEDGED,
                "entrega POS ACKNOWLEDGED");

        assertThat(posIntegrationService.getDelivery(delivery.getId()).getAcknowledgement()).isNotBlank();
    }

    @Test
    void erpBatchReachesDeliveredWithoutOptimisticLockFailure() {
        UUID orderId = UUID.randomUUID();
        erpIntegrationService.registerConfirmedSale(orderId, new BigDecimal("100.00"),
                new BigDecimal("12.00"), new BigDecimal("112.00"), "GTQ", Instant.now());
        ErpBatch batch = erpIntegrationService.runBatch(ErpExportFormat.CSV);

        await(() -> erpIntegrationService.getBatch(batch.getId()).getStatus() == ErpBatchStatus.DELIVERED,
                "lote ERP DELIVERED");

        assertThat(erpIntegrationService.getBatch(batch.getId()).getAcknowledgement()).isNotBlank();
    }

    @Test
    void shipmentLabelIsIssuedWithoutOptimisticLockFailure() {
        Shipment shipment = shipmentService.createDispatch(
                UUID.randomUUID(), DispatchOrigin.DISTRIBUTION_CENTER, "Zona 1, Guatemala",
                UUID.randomUUID(), "it-log-" + UUID.randomUUID());

        await(() -> shipmentService.getShipment(shipment.getId()).getTrackingNumber() != null,
                "guía LogiExpress emitida");

        assertThat(shipmentService.getShipment(shipment.getId()).getTrackingNumber()).startsWith("LGX-");
    }

    @Test
    void notificationReachesSentWithoutOptimisticLockFailure() {
        NotificationRequest request = notificationService.requestNotification(
                NotificationEventType.ORDER_CONFIRMED, NotificationChannel.EMAIL,
                "cliente@example.com", UUID.randomUUID(), UUID.randomUUID());

        await(() -> notificationService.getNotification(request.getId()).getStatus() == NotificationStatus.SENT,
                "notificación SENT");

        assertThat(notificationService.getNotification(request.getId()).getAttempts()).isGreaterThanOrEqualTo(1);
    }

    private static void await(BooleanSupplier condition, String description) {
        long deadline = System.nanoTime() + AWAIT_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Espera interrumpida: " + description, interrupted);
            }
        }
        throw new AssertionError("No se alcanzó el estado esperado: " + description);
    }
}
