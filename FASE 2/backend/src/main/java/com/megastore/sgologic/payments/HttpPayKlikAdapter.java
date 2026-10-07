package com.megastore.sgologic.payments;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * PayKlik real por REST ({@code payklik-service}): autoriza con la llave del
 * comercio y la llave de idempotencia del intento, y consulta por esa llave
 * para conciliar. El tiempo límite de 5 s y el circuit breaker siguen en
 * {@link ResilientPayKlikClient}; aquí solo se traducen las respuestas.
 * Sin URL configurada (pruebas automáticas) se usa {@link SimulatedPayKlikAdapter}.
 */
@Primary
@Component
@ConditionalOnExpression("!'${sgo.integrations.payklik.base-url:}'.isBlank()")
class HttpPayKlikAdapter implements PayKlikGatewayPort {

    private final RestClient restClient;

    HttpPayKlikAdapter(@Value("${sgo.integrations.payklik.base-url}") String baseUrl,
                       @Value("${sgo.integrations.payklik.api-key}") String apiKey,
                       @Value("${sgo.integrations.payklik.read-timeout-ms:15000}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(readTimeoutMs);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory)
                .defaultHeader("X-Api-Key", apiKey).build();
    }

    /** Respuesta de PayKlik (solo los campos que usa SGO-Logic). */
    record PayKlikAuthorization(UUID transactionId, boolean approved, String authorizationCode, String rejectionCode,
                                Instant processedAt) {
        AuthorizationResult toResult() {
            return new AuthorizationResult(transactionId, approved, authorizationCode, rejectionCode, processedAt);
        }
    }

    @Override
    public AuthorizationResult authorize(AuthorizationRequest request) {
        try {
            PayKlikAuthorization body = restClient.post().uri("/api/v1/authorizations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Correlation-Id", String.valueOf(request.correlationId()))
                    .body(Map.of("idempotencyKey", request.idempotencyKey(), "orderReference", request.orderId().toString(),
                            "amount", request.amount(), "currency", request.currency(),
                            "paymentReference", request.paymentReference()))
                    .retrieve()
                    .body(PayKlikAuthorization.class);
            if (body == null) {
                throw new PayKlikUnavailableException("PayKlik respondió sin contenido.");
            }
            return body.toResult();
        } catch (RestClientResponseException error) {
            throw translate(error);
        } catch (RestClientException offline) {
            throw offline(offline);
        }
    }

    @Override
    public Optional<AuthorizationResult> findByIdempotencyKey(String idempotencyKey) {
        try {
            PayKlikAuthorization body = restClient.get().uri("/api/v1/authorizations/{key}", idempotencyKey)
                    .retrieve().body(PayKlikAuthorization.class);
            return Optional.ofNullable(body).map(PayKlikAuthorization::toResult);
        } catch (RestClientResponseException error) {
            if (error.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                return Optional.empty();
            }
            throw translate(error);
        } catch (RestClientException offline) {
            throw offline(offline);
        }
    }

    private static RuntimeException translate(RestClientResponseException error) {
        if (error.getStatusCode().value() == HttpStatus.BAD_REQUEST.value()) {
            // PAN/CVV o datos incompletos: no es una falla del proveedor, no se reintenta.
            return new IllegalArgumentException("PayKlik rechazó la solicitud: " + error.getResponseBodyAsString());
        }
        return new PayKlikUnavailableException("PayKlik respondió " + error.getStatusCode().value() + ".");
    }

    private static RuntimeException offline(RestClientException offline) {
        if (timedOut(offline)) {
            return new PayKlikTimeoutException("PayKlik no respondió a tiempo.");
        }
        return new PayKlikUnavailableException("Sin conexión con PayKlik: " + offline.getMessage());
    }

    private static boolean timedOut(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof SocketTimeoutException) {
                return true;
            }
        }
        return false;
    }
}
