package com.megastore.sgologic.security;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SEC-01 / EaC-12: genera actividad real (pago, incidencia, despacho,
 * notificación, fiscal, auditoría y un 401) y escanea base de datos,
 * mensajes persistidos del Outbox, estados de auditoría y logs en busca de
 * PAN (13–19 dígitos) o marcadores CVV. También comprueba que una
 * referencia con PAN/CVV se rechaza antes de persistir o registrar.
 */
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = "sgo.security.enabled=true")
class SensitiveDataLeakScanTest {

    // PAN de 13-19 digitos con separadores opcionales (espacio, punto o
    // guion), sin consumir separadores finales ni tocar segmentos de UUID.
    private static final Pattern PAN_PATTERN =
            Pattern.compile("(?<![0-9.\\-])\\d(?:[ .\\-]?\\d){12,}(?![0-9.\\-])");
    private static final Pattern CVV_PATTERN = Pattern.compile("(?i)\\b(cvv2?|cvc2?|securitycode)\\b");
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "(?i)\\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\b");
    private static final String PAN_SQL_REGEX = "(?<![0-9.\\-])[0-9]([ .\\-]?[0-9]){12,}(?![0-9.\\-])";
    private static final String CVV_SQL_REGEX = "(?i)\\y(cvv2?|cvc2?|securitycode)\\y";

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
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void noPanOrCvvAppearsInDatabaseMessagesOrLogs() throws Exception {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        ListAppender<ILoggingEvent> logCapture = new ListAppender<>();
        logCapture.start();
        rootLogger.addAppender(logCapture);

        String orderId = UUID.randomUUID().toString();
        try {
            generateActivity(orderId);
        } finally {
            rootLogger.detachAppender(logCapture);
            logCapture.stop();
        }

        ScanResult database = scanDatabase();
        List<String> findings = new ArrayList<>(database.findings());
        findings.addAll(scanLogs(logCapture.list));

        // El escaneo no debe ser vacío: hubo columnas y actividad reales.
        assertThat(database.columnsScanned()).isGreaterThan(20);
        assertThat(logCapture.list).isNotEmpty();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM payment_attempts WHERE order_id = ?::uuid", Integer.class, orderId))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM outbox_event", Integer.class)).isGreaterThanOrEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_events WHERE order_id = ?::uuid", Integer.class, orderId))
                .isGreaterThanOrEqualTo(1);

        assertThat(findings)
                .as("EaC-12: no debe aparecer PAN completo ni CVV en base, mensajes o logs")
                .isEmpty();
    }

    @Test
    void detectorFindsRealCardsButNotUuidsOrTimestamps() {
        assertThat(containsPan("pago con 4111 1111 1111 1111")).isTrue();
        assertThat(containsPan("ref 5500-0000-0000-0004")).isTrue();
        assertThat(containsPan("orden 12345678-1234-4123-8123-123456789012")).isFalse();
        assertThat(containsPan("imagen?v=1791390657671")).isFalse();
    }

    private record ScanResult(List<String> findings, int columnsScanned) {
    }

    private void generateActivity(String orderId) throws Exception {
        String client = login("cliente@megastore.com", "Cliente123");
        String staff = login("staff@megastore.com", "Staff123");
        String finance = login("finanzas@megastore.com", "Finanzas123");

        // Pago aprobado con referencia segura y dos intentos con PAN/CVV rechazados.
        postJson("/api/v1/payments/attempts", client, "sec-pay-1", Map.of(
                "orderId", orderId, "amount", 150.00, "currency", "GTQ", "channel", "MOBILE",
                "method", "CARD", "paymentReference", "tok_test_123")).andExpect(status().isCreated());
        postJson("/api/v1/payments/attempts", client, "sec-pay-pan", Map.of(
                "orderId", orderId, "amount", 150.00, "currency", "GTQ", "channel", "MOBILE",
                "method", "CARD", "paymentReference", "4111111111111111")).andExpect(status().isBadRequest());
        postJson("/api/v1/payments/attempts", client, "sec-pay-pan-dashes", Map.of(
                "orderId", orderId, "amount", 150.00, "currency", "GTQ", "channel", "MOBILE",
                "method", "CARD", "paymentReference", "4111-1111-1111-1111")).andExpect(status().isBadRequest());
        postJson("/api/v1/payments/attempts", client, "sec-pay-pan-spaces", Map.of(
                "orderId", orderId, "amount", 150.00, "currency", "GTQ", "channel", "MOBILE",
                "method", "CARD", "paymentReference", "4111 1111 1111 1111")).andExpect(status().isBadRequest());
        postJson("/api/v1/payments/attempts", client, "sec-pay-cvv", Map.of(
                "orderId", orderId, "amount", 150.00, "currency", "GTQ", "channel", "MOBILE",
                "method", "CARD", "paymentReference", "cvv=123")).andExpect(status().isBadRequest());
        postJson("/api/v1/payments/attempts", client, "sec-pay-cvc2", Map.of(
                "orderId", orderId, "amount", 150.00, "currency", "GTQ", "channel", "MOBILE",
                "method", "CARD", "paymentReference", "cvc2=123")).andExpect(status().isBadRequest());

        // Incidencia registrada por el cliente y resuelta por soporte.
        String incident = mockMvc.perform(post("/api/v1/incidents")
                        .header("Authorization", "Bearer " + client)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "orderId", orderId, "type", "RETURN", "reason", "Prueba SEC-01",
                                "partial", false, "items", List.of(Map.of("sku", "SKU-1", "quantity", 1))))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String incidentId = objectMapper.readTree(incident).get("id").asText();
        mockMvc.perform(patch("/api/v1/incidents/" + incidentId + "/resolution")
                        .header("Authorization", "Bearer " + staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resolution\":\"APPROVED\"}"))
                .andExpect(status().isOk());

        // Despacho con guía y evento de tracking.
        String shipment = mockMvc.perform(post("/api/v1/shipments")
                        .header("Authorization", "Bearer " + staff)
                        .header("Idempotency-Key", "sec-log-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "orderId", orderId, "origin", "DISTRIBUTION_CENTER", "destination", "Zona 1"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String shipmentId = objectMapper.readTree(shipment).get("shipmentId").asText();
        mockMvc.perform(post("/api/v1/shipments/" + shipmentId + "/tracking-events")
                        .header("Authorization", "Bearer " + staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_TRANSIT\"}"))
                .andExpect(status().isOk());

        // Notificación no bloqueante y documento fiscal asíncrono.
        postJson("/api/v1/notifications", staff, null, Map.of(
                "eventType", "SHIPMENT_IN_TRANSIT", "channel", "EMAIL",
                "recipient", "cliente@example.com", "sourceAggregateId", orderId))
                .andExpect(status().isAccepted());
        postJson("/api/v1/fiscal-documents", finance, "sec-fis-" + UUID.randomUUID(), Map.of(
                "orderId", orderId, "paymentAttemptId", UUID.randomUUID().toString(),
                "documentType", "INVOICE", "taxIdentifier", "CF-1234567",
                "customerName", "Cliente Prueba", "total", 150.00, "currency", "GTQ"))
                .andExpect(status().isAccepted());

        // Consulta de evidencia por Finanzas y un 401 sin token.
        mockMvc.perform(get("/api/v1/audit/events").param("orderId", orderId)
                        .header("Authorization", "Bearer " + finance))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/inventory/locations"))
                .andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.ResultActions postJson(String path, String token,
                                                                       String idempotencyKey, Map<String, Object> body)
            throws Exception {
        var request = post(path)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
        if (idempotencyKey != null) {
            request.header("Idempotency-Key", idempotencyKey);
        }
        return mockMvc.perform(request);
    }

    private String login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    /** Escanea todas las columnas de texto/JSON del esquema público. */
    private ScanResult scanDatabase() {
        List<String> findings = new ArrayList<>();
        List<String[]> columns = jdbcTemplate.query(
                "SELECT table_name, column_name FROM information_schema.columns "
                        + "WHERE table_schema = 'public' "
                        + "AND data_type IN ('character varying', 'text', 'character', 'json', 'jsonb')",
                (resultSet, rowNumber) -> new String[]{
                        resultSet.getString("table_name"), resultSet.getString("column_name")});
        for (String[] tableColumn : columns) {
            String table = tableColumn[0];
            String column = tableColumn[1];
            int panMatches = countPans(table, column);
            int cvvMatches = countMatches(table, column, CVV_SQL_REGEX);
            if (panMatches > 0 || cvvMatches > 0) {
                findings.add(table + "." + column + " -> PAN=" + panMatches + ", CVV=" + cvvMatches);
            }
        }
        return new ScanResult(findings, columns.size());
    }

    /**
     * El regex SQL solo preselecciona: un UUID aleatorio o un instante en
     * milisegundos también tienen 13 o más dígitos. Cada candidato se confirma
     * en Java (sin UUID y con dígito verificador Luhn, como toda tarjeta real),
     * para que la prueba no falle al azar sin dejar pasar un PAN verdadero.
     */
    private int countPans(String table, String column) {
        List<String> candidates = jdbcTemplate.queryForList(
                "SELECT " + table + "." + column + "::text FROM " + table + " WHERE " + table + "." + column + "::text ~ ?",
                String.class, PAN_SQL_REGEX);
        return (int) candidates.stream().filter(SensitiveDataLeakScanTest::containsPan).count();
    }

    static boolean containsPan(String value) {
        if (value == null) {
            return false;
        }
        java.util.regex.Matcher candidate = PAN_PATTERN.matcher(UUID_PATTERN.matcher(value).replaceAll("<uuid>"));
        while (candidate.find()) {
            String digits = candidate.group().replaceAll("[^0-9]", "");
            if (digits.length() >= 13 && digits.length() <= 19 && passesLuhn(digits)) {
                return true;
            }
        }
        return false;
    }

    static boolean passesLuhn(String digits) {
        int sum = 0;
        boolean doubleIt = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int digit = digits.charAt(i) - '0';
            if (doubleIt) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            doubleIt = !doubleIt;
        }
        return sum % 10 == 0;
    }

    private int countMatches(String table, String column, String regex) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM " + table + " WHERE " + table + "." + column + "::text ~ ?",
                Integer.class, regex);
        return count == null ? 0 : count;
    }

    private List<String> scanLogs(List<ILoggingEvent> events) {
        List<String> findings = new ArrayList<>();
        for (ILoggingEvent event : events) {
            String message = event.getFormattedMessage();
            // Un UUID aleatorio puede contener trece o más dígitos unidos por
            // guiones y parecer una tarjeta al regex. Se excluyen únicamente
            // UUID completos antes de buscar datos de pago; las referencias
            // numéricas, incluso separadas por guiones, siguen detectándose.
            String scannable = message == null ? null : UUID_PATTERN.matcher(message).replaceAll("<uuid>");
            if (scannable != null && (containsPan(scannable) || CVV_PATTERN.matcher(scannable).find())) {
                findings.add("log -> " + message);
            }
        }
        return findings;
    }
}
