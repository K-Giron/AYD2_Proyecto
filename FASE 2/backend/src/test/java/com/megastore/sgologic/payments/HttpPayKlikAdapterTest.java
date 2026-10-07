package com.megastore.sgologic.payments;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** El adaptador REST de PayKlik contra un servidor HTTP local que imita a payklik-service. */
class HttpPayKlikAdapterTest {

    private HttpServer server;
    private final AtomicReference<String> receivedKey = new AtomicReference<>();
    private final AtomicReference<String> receivedBody = new AtomicReference<>();

    private HttpPayKlikAdapter adapter(int status, String body, long delayMs) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/authorizations", exchange -> {
            receivedKey.set(exchange.getRequestHeaders().getFirst("X-Api-Key"));
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            try { Thread.sleep(delayMs); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
            try (OutputStream out = exchange.getResponseBody()) { out.write(bytes); }
        });
        server.start();
        return new HttpPayKlikAdapter("http://127.0.0.1:" + server.getAddress().getPort(), "llave-comercio", 300);
    }

    @AfterEach
    void stop() {
        if (server != null) server.stop(0);
    }

    private static PayKlikGatewayPort.AuthorizationRequest request() {
        return new PayKlikGatewayPort.AuthorizationRequest(UUID.randomUUID(), new BigDecimal("749.00"), "GTQ",
                "tok_payklik_ab12", UUID.randomUUID(), "pay-key-1");
    }

    @Test
    void approvedChargeComesBackWithTheAuthorizationCode() throws IOException {
        UUID tx = UUID.randomUUID();
        PayKlikGatewayPort.AuthorizationResult result = adapter(201, """
                {"transactionId":"%s","approved":true,"status":"APROBADA","authorizationCode":"PK-00000007",
                 "processedAt":"2026-10-06T20:00:00Z"}""".formatted(tx), 0).authorize(request());

        assertThat(result.approved()).isTrue();
        assertThat(result.providerReference()).isEqualTo("PK-00000007");
        assertThat(result.paymentAttemptId()).isEqualTo(tx);
        assertThat(receivedKey.get()).isEqualTo("llave-comercio");
        assertThat(receivedBody.get()).contains("\"idempotencyKey\":\"pay-key-1\"", "\"paymentReference\":\"tok_payklik_ab12\"");
    }

    @Test
    void outageAndSlownessAreRetryableFailures() throws IOException {
        assertThatThrownBy(() -> adapter(503, "{\"code\":\"SERVICE_UNAVAILABLE\"}", 0).authorize(request()))
                .isInstanceOf(PayKlikGatewayPort.PayKlikUnavailableException.class);
        stop();
        assertThatThrownBy(() -> adapter(201, "{}", 1500).authorize(request()))
                .isInstanceOf(PayKlikGatewayPort.PayKlikTimeoutException.class);
    }

    @Test
    void rejectedRequestIsNotAProviderFailure() throws IOException {
        assertThatThrownBy(() -> adapter(400, "{\"code\":\"SENSITIVE_DATA\"}", 0).authorize(request()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unknownKeyMeansThereIsNothingToReconcile() throws IOException {
        assertThat(adapter(404, "{\"code\":\"AUTHORIZATION_NOT_FOUND\"}", 0).findByIdempotencyKey("nunca")).isEmpty();
    }
}
