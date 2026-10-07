package com.megastore.sgologic.onlinestore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Baja del almacén central lo que le enviaron a POS-ONLINE, con la cuenta
 * de equipo de la tienda online (rol TIENDA). Mismo sentido de
 * dependencia que el resto de ADR-015: el monolito llama al central,
 * nunca al revés.
 */
@Component
class CentralReceiptsClient {

    private final RestClient restClient;
    private final OnlineStoreProperties properties;
    private volatile String token;

    CentralReceiptsClient(OnlineStoreProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) properties.getTimeoutMs());
        requestFactory.setReadTimeout((int) properties.getTimeoutMs());
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    List<IncomingReceipt> receiptsSince(Instant since) {
        return call(() -> fetch(since));
    }

    /** Puntos de venta del central (destinos posibles de un traslado). */
    List<PointOfSale> pointsOfSale() {
        return call(() -> {
            PointOfSale[] body = restClient.get().uri("/api/v1/points-of-sale")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                    .retrieve().body(PointOfSale[].class);
            return body == null ? List.<PointOfSale>of() : Arrays.asList(body);
        });
    }

    /**
     * Registra en el central un traslado de la tienda online (idempotente por
     * el folio local). Un 4xx es rechazo de negocio ({@code TRANSFER_REJECTED});
     * red o 5xx es falta de conexión ({@code CENTRAL_UNAVAILABLE}).
     */
    TransferReceipt registerTransfer(TransferBody transfer) {
        return call(() -> {
            try {
                return restClient.post()
                        .uri("/api/v1/store-sync/{pos}/transfers", properties.getPointOfSaleCode())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(transfer)
                        .retrieve()
                        .body(TransferReceipt.class);
            } catch (RestClientResponseException error) {
                int status = error.getStatusCode().value();
                if (status >= 400 && status < 500 && status != HttpStatus.UNAUTHORIZED.value()) {
                    throw new OnlineStoreException(HttpStatus.CONFLICT, "TRANSFER_REJECTED", rejectionMessage(error));
                }
                throw error;
            }
        });
    }

    private <T> T call(java.util.function.Supplier<T> request) {
        try {
            try {
                return request.get();
            } catch (RestClientResponseException unauthorized) {
                if (unauthorized.getStatusCode().value() != HttpStatus.UNAUTHORIZED.value()) {
                    throw unauthorized;
                }
                token = null;
                return request.get();
            }
        } catch (RestClientException error) {
            throw unavailable(error);
        }
    }

    private static String rejectionMessage(RestClientResponseException error) {
        try {
            ErrorBody body = error.getResponseBodyAs(ErrorBody.class);
            if (body != null && body.message() != null) {
                return body.message();
            }
        } catch (RuntimeException unreadable) {
            // cuerpo vacío o no JSON
        }
        return "El almacén central rechazó el traslado (" + error.getStatusCode().value() + ").";
    }

    private List<IncomingReceipt> fetch(Instant since) {
        ReceiptBody[] body = restClient.get()
                .uri(uri -> {
                    uri.path("/api/v1/store-sync/{pos}/receipts");
                    if (since != null) {
                        uri.queryParam("since", since.toString());
                    }
                    return uri.build(properties.getPointOfSaleCode());
                })
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                .retrieve()
                .body(ReceiptBody[].class);
        return Arrays.stream(body == null ? new ReceiptBody[0] : body)
                .map(r -> new IncomingReceipt(r.folio(), r.originFolio(), r.origin(), r.articleId(), r.sku(), r.name(),
                        r.description(), r.price(), r.quantity(), r.occurredAt()))
                .toList();
    }

    private String token() {
        String current = token;
        if (current == null) {
            LoginBody login = restClient.post()
                    .uri("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new LoginRequest(properties.getUsername(), properties.getPassword()))
                    .retrieve()
                    .body(LoginBody.class);
            if (login == null || login.accessToken() == null) {
                throw new RestClientException("El almacén central no devolvió sesión.");
            }
            current = login.accessToken();
            token = current;
        }
        return current;
    }

    private static OnlineStoreException unavailable(Exception cause) {
        return new OnlineStoreException(HttpStatus.BAD_GATEWAY, "CENTRAL_UNAVAILABLE",
                "No hay conexión con el almacén central: " + cause.getMessage());
    }

    private record LoginRequest(String username, String password) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ErrorBody(String code, String message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PointOfSale(String code, String name) {
    }

    record TransferBody(String localFolio, UUID articleId, int quantity, String destination, String reason,
                        String seller) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TransferReceipt(String centralFolio, String localFolio, String destinationFolio) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LoginBody(String accessToken) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ReceiptBody(String folio, String originFolio, String origin, UUID articleId, String sku, String name,
                               String description, BigDecimal price, int quantity, Instant occurredAt) {
    }
}
