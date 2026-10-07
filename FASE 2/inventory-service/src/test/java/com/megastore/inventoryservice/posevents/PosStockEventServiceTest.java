package com.megastore.inventoryservice.posevents;

import com.megastore.inventoryservice.article.ArticleRepository;
import com.megastore.inventoryservice.auditlog.AuditLogService;
import com.megastore.inventoryservice.pointofsale.PointOfSaleService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Entrega de eventos de stock a un POS real (servidor HTTP local), con reintentos y orden por tienda. */
class PosStockEventServiceTest {

    private HttpServer server;
    private final List<String> received = new ArrayList<>();
    private final AtomicInteger failuresLeft = new AtomicInteger();
    private final PosStockEventRepository repository = mock(PosStockEventRepository.class);
    private final AuditLogService auditLog = mock(AuditLogService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-06T20:00:00Z"), ZoneOffset.UTC);
    private final List<PosStockEvent> events = new ArrayList<>();

    private PosStockEventService service(boolean connected) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/store/stock-events", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            int status = failuresLeft.getAndDecrement() > 0 ? 503 : 200;
            if (status == 200) received.add(body);
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        server.start();
        PosTerminalProperties properties = new PosTerminalProperties();
        PosTerminalProperties.Terminal terminal = new PosTerminalProperties.Terminal();
        terminal.setUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/api/store/stock-events");
        if (connected) properties.setTerminals(Map.of("POS-01", terminal));
        properties.setTimeoutMs(1000);
        when(repository.findTop100ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(any(), any()))
                .thenAnswer(invocation -> events.stream()
                        .filter(e -> List.of(PosStockEvent.PENDING, PosStockEvent.RETRY, PosStockEvent.NO_TERMINAL).contains(e.getStatus()))
                        .toList());
        return new PosStockEventService(repository, mock(ArticleRepository.class), mock(PointOfSaleService.class),
                properties, new PosTerminalClient(properties), auditLog, clock);
    }

    @AfterEach
    void stop() {
        if (server != null) server.stop(0);
    }

    private PosStockEvent event(String folio) {
        PosStockEvent event = PosStockEvent.of(UUID.randomUUID(), "POS-01", UUID.randomUUID(), "SK-MAX-95", "Tenis",
                "VENTA", -2, 3, folio, "FT-000001", clock.instant());
        events.add(event);
        return event;
    }

    @Test
    void theStoreReceivesItsStockChange() throws IOException {
        PosStockEvent sale = event("POS-VTA-000001");

        assertThat(service(true).deliverDue()).isEqualTo(1);

        assertThat(sale.getStatus()).isEqualTo(PosStockEvent.DELIVERED);
        assertThat(sale.getAcknowledgement()).startsWith("POS-01-ACK-");
        assertThat(received).singleElement().asString()
                .contains("\"movement\":\"VENTA\"", "\"delta\":-2", "\"quantityAfter\":3", "\"folio\":\"POS-VTA-000001\"");
    }

    @Test
    void storeDownRetriesAndKeepsTheOrderUntilItGivesUp() throws IOException {
        PosStockEvent first = event("POS-VTA-000001");
        PosStockEvent second = event("POS-VTA-000002");
        failuresLeft.set(10);
        PosStockEventService service = service(true);

        service.deliverDue();
        assertThat(first.getStatus()).isEqualTo(PosStockEvent.RETRY);
        assertThat(first.getNextAttemptAt()).isEqualTo(clock.instant().plusSeconds(2));
        // El segundo de la misma tienda espera a que pase el primero.
        assertThat(second.getAttempts()).isZero();

        service.deliverDue();
        service.deliverDue();
        service.deliverDue();
        assertThat(first.getStatus()).isEqualTo(PosStockEvent.FAILED);
        assertThat(first.getAttempts()).isEqualTo(4);
        verify(auditLog).record(eq("POS_EVENTO_FALLIDO"), anyString(), eq("POS-VTA-000001"), any());

        // La tienda vuelve: el reenvío manual entrega.
        failuresLeft.set(0);
        when(repository.findById(first.getId())).thenReturn(java.util.Optional.of(first));
        service.retry(first.getId());
        service.deliverDue();
        assertThat(first.getStatus()).isEqualTo(PosStockEvent.DELIVERED);
    }

    @Test
    void pointOfSaleWithoutASystemKeepsItsEventsWaiting() throws IOException {
        PosStockEvent sale = event("POS-VTA-000003");

        service(false).deliverDue();

        assertThat(sale.getStatus()).isEqualTo(PosStockEvent.NO_TERMINAL);
        assertThat(received).isEmpty();
    }
}
