package com.megastore.sgologic.logistics;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** El adaptador REST de LogiExpress contra un servidor HTTP local que imita a logiexpress-service. */
class HttpLogiExpressAdapterTest {

    private HttpServer server;
    private final AtomicReference<String> receivedBody = new AtomicReference<>();

    private HttpLogiExpressAdapter adapter(int status, String body, long delayMs) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/guides", exchange -> {
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            try { Thread.sleep(delayMs); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) { out.write(bytes); }
        });
        server.start();
        return new HttpLogiExpressAdapter("http://127.0.0.1:" + server.getAddress().getPort(), "llave", 300);
    }

    @AfterEach
    void stop() {
        if (server != null) server.stop(0);
    }

    @Test
    void pickupOrdersAskForDeliveryToTheStore() throws Exception {
        UUID shipmentId = UUID.randomUUID();
        LogiExpressLabelResult result = adapter(201, "{\"trackingNumber\":\"LGX-00000001\",\"status\":\"CREADA\"}", 0)
                .requestLabel(shipmentId, UUID.randomUUID(), "Almacén CD-WEB",
                        "Recoger en Tienda Centro (tienda física 1) (POS-01) — 6a. Avenida", UUID.randomUUID());

        assertThat(result.trackingNumber()).isEqualTo("LGX-00000001");
        assertThat(receivedBody.get()).contains("\"merchantReference\":\"" + shipmentId + "\"",
                "\"pickupPoint\":\"POS-01\"", "\"origin\":\"Almacén CD-WEB\"");
    }

    @Test
    void outageAndSlownessAreRetried() throws Exception {
        assertThatThrownBy(() -> adapter(503, "{}", 0).requestLabel(UUID.randomUUID(), UUID.randomUUID(), "x", "Zona 10", UUID.randomUUID()))
                .isInstanceOf(LogiExpressUnavailableException.class);
        stop();
        assertThatThrownBy(() -> adapter(201, "{}", 1500).requestLabel(UUID.randomUUID(), UUID.randomUUID(), "x", "Zona 10", UUID.randomUUID()))
                .isInstanceOf(LogiExpressTimeoutException.class);
    }

    @Test
    void trackingReturnsTheCarrierStatus() throws Exception {
        UUID shipmentId = UUID.randomUUID();
        var guides = adapter(200, """
                [{"trackingNumber":"LGX-00000002","merchantReference":"%s","status":"ENTREGADA","updatedAt":"2026-10-06T20:00:00Z"}]
                """.formatted(shipmentId), 0).guidesUpdatedSince(Instant.EPOCH);

        assertThat(guides).singleElement().satisfies(guide -> {
            assertThat(guide.merchantReference()).isEqualTo(shipmentId.toString());
            assertThat(guide.status()).isEqualTo("ENTREGADA");
        });
    }
}
