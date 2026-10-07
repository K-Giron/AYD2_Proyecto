package com.megastore.inventoryservice.posevents;

import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Entrega por HTTP un evento de stock al sistema del punto de venta. Si el
 * terminal exige sesión (la tienda online vive en SGO-Logic), el central
 * inicia sesión con su cuenta de servicio y reusa el token hasta que expire.
 */
@Component
class PosTerminalClient {

    private final RestClient restClient;
    private final Map<String, String> tokens = new ConcurrentHashMap<>();

    PosTerminalClient(PosTerminalProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Math.min(properties.getTimeoutMs(), 3000));
        requestFactory.setReadTimeout(properties.getTimeoutMs());
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    /** Devuelve el acuse; lanza {@link DeliveryException} si el POS no lo recibió. */
    String deliver(PosTerminalProperties.Terminal terminal, PosStockEvent event) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("eventId", event.getId().toString());
        body.put("posCode", event.getPosCode());
        body.put("articleId", event.getArticleId().toString());
        body.put("sku", event.getSku());
        body.put("name", event.getArticleName());
        body.put("movement", event.getMovement());
        body.put("delta", event.getDelta());
        body.put("quantityAfter", event.getQuantityAfter());
        body.put("folio", event.getFolio());
        body.put("reference", event.getReference());
        body.put("occurredAt", event.getCreatedAt().toString());
        try {
            return post(terminal, event.getPosCode(), body, false);
        } catch (RestClientResponseException error) {
            throw new DeliveryException("El POS respondió " + error.getStatusCode().value() + ": "
                    + error.getResponseBodyAsString());
        } catch (RestClientException offline) {
            throw new DeliveryException("Sin conexión con el POS " + event.getPosCode() + ": " + offline.getMessage());
        }
    }

    private String post(PosTerminalProperties.Terminal terminal, String posCode, Map<String, Object> body, boolean retried) {
        var request = restClient.post().uri(terminal.getUrl()).contentType(MediaType.APPLICATION_JSON);
        if (terminal.requiresLogin()) {
            request = request.header("Authorization", "Bearer " + tokens.computeIfAbsent(posCode, code -> login(terminal)));
        }
        try {
            request.body(body).retrieve().toBodilessEntity();
            return posCode + "-ACK-" + body.get("eventId");
        } catch (RestClientResponseException error) {
            if (error.getStatusCode().value() == 401 && terminal.requiresLogin() && !retried) {
                tokens.remove(posCode);
                return post(terminal, posCode, body, true);
            }
            throw error;
        }
    }

    @SuppressWarnings("unchecked")
    private String login(PosTerminalProperties.Terminal terminal) {
        Map<String, Object> response = restClient.post().uri(terminal.getLoginUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("username", terminal.getUsername(), "password", terminal.getPassword()))
                .retrieve().body(Map.class);
        Object token = response == null ? null : response.get("accessToken");
        if (token == null) {
            throw new DeliveryException("El POS no entregó sesión para el central.");
        }
        return token.toString();
    }

    static class DeliveryException extends RuntimeException {
        DeliveryException(String message) {
            super(message);
        }
    }
}
