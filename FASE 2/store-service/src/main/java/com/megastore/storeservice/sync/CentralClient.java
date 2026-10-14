package com.megastore.storeservice.sync;

import com.megastore.storeservice.inventory.IncomingReceipt;
import com.megastore.storeservice.shared.StoreProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Cliente del almacén central (inventory-service) con la cuenta de equipo
 * de esta tienda (rol TIENDA). El token se reutiliza hasta que el central
 * lo rechace; entonces se vuelve a iniciar sesión una vez.
 */
@Component
public class CentralClient {

    private final RestClient restClient;
    private final StoreProperties properties;
    private volatile String token;

    public CentralClient(RestClient.Builder builder, StoreProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = builder.clone().baseUrl(properties.central().url()).requestFactory(requestFactory).build();
        this.properties = properties;
    }

    public List<IncomingReceipt> receipts(Instant since) {
        return call(() -> restClient.get()
                .uri(uri -> uri.path("/api/v1/store-sync/{pos}/receipts")
                        .queryParamIfPresent("since", java.util.Optional.ofNullable(since).map(Instant::toString))
                        .build(properties.code()))
                .header("Authorization", "Bearer " + token())
                .retrieve()
                .body(new ParameterizedTypeReference<List<IncomingReceipt>>() { }));
    }

    public InvoiceReceipt uploadInvoice(Map<String, Object> invoice) {
        return call(() -> restClient.post()
                .uri("/api/v1/store-sync/{pos}/invoices", properties.code())
                .header("Authorization", "Bearer " + token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(invoice)
                .retrieve()
                .body(InvoiceReceipt.class));
    }

    public TransferReceipt registerTransfer(Map<String, Object> transfer) {
        return call(() -> restClient.post()
                .uri("/api/v1/store-sync/{pos}/transfers", properties.code())
                .header("Authorization", "Bearer " + token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(transfer)
                .retrieve()
                .body(TransferReceipt.class));
    }

    /** Puntos de venta del central, para elegir el destino de un traslado. */
    public List<PointOfSale> pointsOfSale() {
        return call(() -> restClient.get()
                .uri("/api/v1/points-of-sale")
                .header("Authorization", "Bearer " + token())
                .retrieve()
                .body(new ParameterizedTypeReference<List<PointOfSale>>() { }));
    }

    private <T> T call(Supplier<T> request) {
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
        } catch (ResourceAccessException offline) {
            throw new CentralUnavailableException("Sin conexión con el almacén central (" + properties.central().url() + ").", offline);
        } catch (RestClientResponseException error) {
            if (error.getStatusCode().is5xxServerError()) {
                throw new CentralUnavailableException("El almacén central respondió " + error.getStatusCode().value() + ".", error);
            }
            throw new CentralRejectedException(error.getStatusCode().value(), describe(error));
        }
    }

    private String token() {
        String current = token;
        if (current == null) {
            LoginResponse login = restClient.post()
                    .uri("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("username", properties.central().username(), "password", properties.central().password()))
                    .retrieve()
                    .body(LoginResponse.class);
            current = login.accessToken();
            token = current;
        }
        return current;
    }

    /** El central devuelve {code, message, ...}; se guarda el mensaje legible. */
    static String describe(RestClientResponseException error) {
        try {
            ErrorBody body = error.getResponseBodyAs(ErrorBody.class);
            if (body != null && body.message() != null) {
                return body.message();
            }
        } catch (RuntimeException unreadable) {
            // cuerpo vacío o no JSON: queda el mensaje genérico
        }
        return "El central respondió " + error.getStatusCode().value() + ".";
    }

    record ErrorBody(String code, String message) {
    }

    record LoginResponse(String accessToken) {
    }

    public record InvoiceReceipt(String centralFolio, String ticketFolio, Instant receivedAt) {
    }

    public record TransferReceipt(String centralFolio, String localFolio, String destinationFolio) {
    }

    public record PointOfSale(UUID id, String code, String name) {
    }

    /** Línea tal como la espera el central (UploadStoreInvoiceRequest.Line). */
    public record InvoiceLine(UUID articleId, String sku, String name, BigDecimal unitPrice, int quantity,
                              BigDecimal lineTotal) {
    }
}
