package com.megastore.sgologic.integration;

import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.UUID;

/**
 * Entrega de eventos a los POS locales: si la tienda tiene un endpoint real
 * configurado (su sistema de tienda), el evento se envía por HTTP; si no, va
 * al simulador BB-06. Un error de red o 5xx es "POS no disponible" (reintento
 * 2/8/32 s y bandeja de fallidos); un 4xx es rechazo del POS.
 */
@Primary
@Component
class RoutingPosGatewayAdapter implements PosGatewayPort {

    private final SimulatedPosGatewayAdapter simulated;
    private final PosStoreEndpointsProperties properties;
    private final RestClient restClient;

    RoutingPosGatewayAdapter(SimulatedPosGatewayAdapter simulated, PosStoreEndpointsProperties properties) {
        this.simulated = simulated;
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) properties.getTimeoutMs());
        requestFactory.setReadTimeout((int) properties.getTimeoutMs());
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    @Override
    public DeliveryResult deliver(int storeNumber, UUID eventId, String eventType, UUID orderId, String payload,
                                  UUID correlationId) {
        String endpoint = properties.getStoreEndpoints().get(storeNumber);
        if (endpoint == null || endpoint.isBlank()) {
            return simulated.deliver(storeNumber, eventId, eventType, orderId, payload, correlationId);
        }
        try {
            restClient.post().uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Correlation-Id", correlationId == null ? "" : correlationId.toString())
                    .body(Map.of("eventId", eventId.toString(), "eventType", eventType, "orderId", orderId.toString(),
                            "storeNumber", storeNumber, "payload", payload))
                    .retrieve()
                    .toBodilessEntity();
            return new DeliveryResult(true, "POS-" + String.format("%02d", storeNumber) + "-ACK-" + eventId, null);
        } catch (RestClientResponseException error) {
            if (error.getStatusCode().is4xxClientError()) {
                return new DeliveryResult(false, null, "POS_REJECTED_" + error.getStatusCode().value());
            }
            throw new PosUnavailableException("El POS de la tienda " + storeNumber + " respondió "
                    + error.getStatusCode().value());
        } catch (ResourceAccessException offline) {
            throw new PosUnavailableException("Sin conexión con el POS de la tienda " + storeNumber + ": "
                    + offline.getMessage());
        }
    }
}
