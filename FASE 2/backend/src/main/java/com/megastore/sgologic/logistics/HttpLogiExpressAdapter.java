package com.megastore.sgologic.logistics;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LogiExpress real por REST ({@code logiexpress-service}): genera la guía
 * (idempotente por el id del despacho) y entrega el rastreo incremental. Un
 * timeout o un 5xx/503 es "no disponible" para que {@link ShipmentService}
 * reintente 2/8/32 s; sin URL configurada se usa el simulador.
 */
@Primary
@Component
@ConditionalOnExpression("!'${sgo.integrations.logiexpress.base-url:}'.isBlank()")
class HttpLogiExpressAdapter implements LogiExpressGatewayPort, LogiExpressTrackingPort {

    private static final Pattern PICKUP_STORE = Pattern.compile("\\((POS-\\d{2})\\)");

    private final RestClient restClient;

    HttpLogiExpressAdapter(@Value("${sgo.integrations.logiexpress.base-url}") String baseUrl,
                           @Value("${sgo.integrations.logiexpress.api-key}") String apiKey,
                           @Value("${sgo.integrations.logiexpress.timeout-ms:5000}") int timeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Math.min(timeoutMs, 3000));
        requestFactory.setReadTimeout(timeoutMs);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory)
                .defaultHeader("X-Api-Key", apiKey).build();
    }

    record Guide(String trackingNumber, String merchantReference, String status, String pickupPoint, Instant updatedAt) {
    }

    @Override
    public LogiExpressLabelResult requestLabel(UUID shipmentId, UUID orderId, String destination, UUID correlationId)
            throws LogiExpressTimeoutException, LogiExpressUnavailableException {
        return requestLabel(shipmentId, orderId, "MegaStore", destination, correlationId);
    }

    @Override
    public LogiExpressLabelResult requestLabel(UUID shipmentId, UUID orderId, String origin, String destination,
                                               UUID correlationId)
            throws LogiExpressTimeoutException, LogiExpressUnavailableException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("merchantReference", shipmentId.toString());
        body.put("orderReference", orderId.toString());
        body.put("origin", origin);
        body.put("destination", destination);
        Matcher pickup = PICKUP_STORE.matcher(destination == null ? "" : destination);
        if (pickup.find()) {
            body.put("pickupPoint", pickup.group(1));
        }
        try {
            Guide guide = restClient.post().uri("/api/v1/guides")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Correlation-Id", String.valueOf(correlationId))
                    .body(body).retrieve().body(Guide.class);
            if (guide == null || guide.trackingNumber() == null) {
                throw new LogiExpressUnavailableException("LogiExpress respondió sin número de guía.");
            }
            return new LogiExpressLabelResult(guide.trackingNumber());
        } catch (RestClientResponseException error) {
            throw new LogiExpressUnavailableException("LogiExpress respondió " + error.getStatusCode().value()
                    + " para el despacho " + shipmentId + ".");
        } catch (RestClientException offline) {
            if (timedOut(offline)) {
                throw new LogiExpressTimeoutException("LogiExpress no confirmó la guía a tiempo para el despacho " + shipmentId);
            }
            throw new LogiExpressUnavailableException("Sin conexión con LogiExpress: " + offline.getMessage());
        }
    }

    private static boolean timedOut(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof SocketTimeoutException) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<TrackedGuide> guidesUpdatedSince(Instant since) throws LogiExpressUnavailableException {
        try {
            Guide[] guides = restClient.get()
                    .uri(uri -> uri.path("/api/v1/guides").queryParam("updatedSince", since.toString()).build())
                    .retrieve().body(Guide[].class);
            if (guides == null) {
                return List.of();
            }
            return java.util.Arrays.stream(guides)
                    .map(g -> new TrackedGuide(g.trackingNumber(), g.merchantReference(), g.status(), g.updatedAt()))
                    .toList();
        } catch (RestClientException error) {
            throw new LogiExpressUnavailableException("No se pudo consultar el rastreo de LogiExpress: " + error.getMessage());
        }
    }
}
