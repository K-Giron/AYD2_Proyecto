package com.megastore.storeservice.webcart;

import com.megastore.storeservice.shared.StoreBusinessException;
import com.megastore.storeservice.shared.StoreProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Cliente de la tienda web (sgo-logic) con la cuenta de la terminal
 * (rol SUPPORT). Solo se usa si el cliente quiere identificarse para
 * cobrar en tienda lo que dejó en su carrito web; vender no depende de esto.
 */
@Component
public class MonolithClient {

    private final RestClient restClient;
    private final StoreProperties properties;
    private volatile String token;

    public MonolithClient(RestClient.Builder builder, StoreProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = builder.clone().baseUrl(properties.monolith().url()).requestFactory(requestFactory).build();
        this.properties = properties;
    }

    public Optional<Customer> findCustomer(String username) {
        return optional(() -> restClient.get()
                .uri(uri -> uri.path("/api/v1/customers/by-username").queryParam("username", username).build())
                .header("Authorization", "Bearer " + token())
                .retrieve()
                .body(Customer.class));
    }

    public Optional<Cart> activeCart(UUID customerId) {
        return optional(() -> restClient.get()
                .uri(uri -> uri.path("/api/v1/carts/active").queryParam("customerId", customerId).build())
                .header("Authorization", "Bearer " + token())
                .retrieve()
                .body(Cart.class));
    }

    /** Al cobrarlo en tienda, el carrito web se cierra para que el cliente no lo vuelva a pagar en la web. */
    public void abandonCart(UUID cartId, String actor) {
        call(() -> restClient.delete()
                .uri("/api/v1/carts/{cartId}", cartId)
                .header("Authorization", "Bearer " + token())
                .header("X-Actor", actor)
                .retrieve()
                .toBodilessEntity());
    }

    /**
     * Retiro en tienda: avisa a SGO-Logic que el cliente recogió aquí su pedido
     * web (cuenta de la terminal, rol STORE). Con eso la orden queda entregada.
     */
    public void handOverPickup(UUID orderId, String storeCode, String receivedBy) {
        call(() -> restClient.post()
                .uri("/api/v1/store-pickups/{orderId}/handoff", orderId)
                .header("Authorization", "Bearer " + token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(java.util.Map.of("store", storeCode, "receivedBy", receivedBy))
                .retrieve()
                .toBodilessEntity());
    }

    private <T> Optional<T> optional(Supplier<T> request) {
        try {
            return Optional.ofNullable(call(request));
        } catch (StoreBusinessException notFound) {
            if (notFound.getStatus() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw notFound;
        }
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
            throw new StoreBusinessException(HttpStatus.BAD_GATEWAY, "WEB_STORE_UNAVAILABLE",
                    "No hay conexión con la tienda web; se puede vender igual sin traer el carrito.");
        } catch (RestClientResponseException error) {
            if (error.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                throw new StoreBusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND", "No encontrado en la tienda web.");
            }
            if (error.getStatusCode().value() == HttpStatus.CONFLICT.value()) {
                java.util.regex.Matcher message = java.util.regex.Pattern.compile("\"message\"\\s*:\\s*\"([^\"]*)\"")
                        .matcher(error.getResponseBodyAsString());
                throw new StoreBusinessException(HttpStatus.CONFLICT, "WEB_STORE_CONFLICT",
                        message.find() ? message.group(1) : "La tienda web rechazó la operación.");
            }
            throw new StoreBusinessException(HttpStatus.BAD_GATEWAY, "WEB_STORE_ERROR",
                    "La tienda web respondió " + error.getStatusCode().value() + ".");
        }
    }

    private String token() {
        String current = token;
        if (current == null) {
            LoginResponse login = restClient.post()
                    .uri("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("username", properties.monolith().username(), "password", properties.monolith().password()))
                    .retrieve()
                    .body(LoginResponse.class);
            current = login.accessToken();
            token = current;
        }
        return current;
    }

    record LoginResponse(String accessToken) {
    }

    public record Customer(UUID id, String username, String fullName) {
    }

    public record Cart(UUID id, String status, String originChannel, List<CartItem> items, BigDecimal total) {
    }

    public record CartItem(UUID productId, String productName, BigDecimal unitPrice, int quantity) {
    }
}
