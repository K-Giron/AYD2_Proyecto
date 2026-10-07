package com.megastore.sgologic.inventorysync;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Única implementación real de {@link InventoryStockPort}: inicia sesión
 * en inventory-service con una cuenta de servicio propia, resuelve el
 * catálogo (nombre de producto → articleId) y el punto de venta
 * "Tienda Online" una sola vez cada {@link #CATALOG_TTL}, y registra cada
 * línea vendida como venta del punto de venta (POS-ONLINE para la web)
 * con referencia externa idempotente.
 */
@Component
class InventoryServiceStockAdapter implements InventoryStockPort {

    private static final String ONLINE_POINT_OF_SALE_CODE = "POS-ONLINE";
    private static final Duration CATALOG_TTL = Duration.ofSeconds(60);

    private final RestClient restClient;
    private final InventoryServiceSyncProperties properties;
    private final Clock clock;

    private volatile String cachedToken;
    private volatile Instant cachedTokenExpiry;
    private volatile Map<String, UUID> articleIdByName = Map.of();
    private volatile java.util.Set<UUID> knownArticleIds = java.util.Set.of();
    private volatile UUID onlinePointOfSaleId;
    private volatile Map<String, UUID> pointOfSaleIdByCode = Map.of();
    private volatile Instant catalogLoadedAt;

    InventoryServiceStockAdapter(InventoryServiceSyncProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) properties.getTimeoutMs());
        requestFactory.setReadTimeout((int) properties.getTimeoutMs());
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public String registerSale(SoldLine line, UUID orderId) {
        return registerPointOfSaleSale(line, orderId, ONLINE_POINT_OF_SALE_CODE,
                "WEB:" + orderId + ":" + line.productId(), "Venta web — orden " + orderId);
    }

    @Override
    public String registerStoreSale(SoldLine line, UUID orderId, String pointOfSaleCode) {
        return registerPointOfSaleSale(line, orderId, pointOfSaleCode,
                "STORE:" + orderId + ":" + line.productId(),
                "Venta en tienda " + pointOfSaleCode + " — orden " + orderId);
    }

    /**
     * Venta contra la existencia propia de un punto de venta (web = POS-ONLINE,
     * o una tienda física). Sin existencia suficiente no hay reintento: es un
     * dato, no una falla de red.
     */
    private String registerPointOfSaleSale(SoldLine line, UUID orderId, String pointOfSaleCode,
                                           String externalReference, String reason) {
        ensureCatalogLoaded();
        // Los productos de la tienda online usan como id el del artículo del central
        // (onlinestore); los de demostración se siguen cruzando por nombre.
        UUID articleId = knownArticleIds.contains(line.productId())
                ? line.productId()
                : articleIdByName.get(line.productName());
        if (articleId == null) {
            throw new InventoryArticleNotFoundException(line.productName());
        }
        UUID pointOfSaleId = pointOfSaleIdByCode.get(pointOfSaleCode);
        if (pointOfSaleId == null) {
            throw new InventoryStoreSaleRejectedException(
                    "inventory-service no tiene el punto de venta " + pointOfSaleCode + ".");
        }
        try {
            PosSaleResponse response = restClient.post()
                    .uri("/api/v1/pos-stock/sales")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + ensureToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new RegisterPosSaleRequest(pointOfSaleId, articleId, line.quantity(), reason,
                            externalReference))
                    .retrieve()
                    .body(PosSaleResponse.class);
            return response == null ? null : response.folio();
        } catch (HttpClientErrorException.Conflict | HttpClientErrorException.NotFound rejected) {
            throw new InventoryStoreSaleRejectedException("El punto de venta " + pointOfSaleCode
                    + " no pudo registrar la venta de \"" + line.productName() + "\": "
                    + rejected.getResponseBodyAsString());
        } catch (RestClientException e) {
            throw new InventoryServiceUnavailableException(
                    "No se pudo registrar la venta de " + pointOfSaleCode + " en inventory-service para la orden "
                            + orderId, e);
        }
    }

    private synchronized void ensureCatalogLoaded() {
        Instant now = clock.instant();
        if (catalogLoadedAt != null && now.isBefore(catalogLoadedAt.plus(CATALOG_TTL))) {
            return;
        }
        String token = ensureToken();
        try {
            ArticleSummary[] articles = restClient.get().uri("/api/v1/articles")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(ArticleSummary[].class);
            PointOfSaleSummary[] pointsOfSale = restClient.get().uri("/api/v1/points-of-sale")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(PointOfSaleSummary[].class);

            articleIdByName = Arrays.stream(articles == null ? new ArticleSummary[0] : articles)
                    .collect(Collectors.toMap(ArticleSummary::name, ArticleSummary::id, (a, b) -> a));
            knownArticleIds = Arrays.stream(articles == null ? new ArticleSummary[0] : articles)
                    .map(ArticleSummary::id)
                    .collect(Collectors.toUnmodifiableSet());
            pointOfSaleIdByCode = Arrays.stream(pointsOfSale == null ? new PointOfSaleSummary[0] : pointsOfSale)
                    .collect(Collectors.toMap(PointOfSaleSummary::code, PointOfSaleSummary::id, (a, b) -> a));
            onlinePointOfSaleId = Optional.ofNullable(pointOfSaleIdByCode.get(ONLINE_POINT_OF_SALE_CODE))
                    .orElseThrow(() -> new InventoryServiceUnavailableException(
                            "inventory-service no tiene sembrado el punto de venta "
                                    + ONLINE_POINT_OF_SALE_CODE, null));
            catalogLoadedAt = now;
        } catch (RestClientException e) {
            throw new InventoryServiceUnavailableException(
                    "No se pudo cargar el catálogo de inventory-service", e);
        }
    }

    private synchronized String ensureToken() {
        Instant now = clock.instant();
        if (cachedToken != null && cachedTokenExpiry != null && now.isBefore(cachedTokenExpiry.minusSeconds(30))) {
            return cachedToken;
        }
        try {
            LoginResponse response = restClient.post()
                    .uri("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new LoginRequest(properties.getUsername(), properties.getPassword()))
                    .retrieve()
                    .body(LoginResponse.class);
            if (response == null) {
                throw new InventoryServiceUnavailableException("inventory-service no devolvió sesión", null);
            }
            cachedToken = response.accessToken();
            cachedTokenExpiry = response.expiresAt();
            return cachedToken;
        } catch (RestClientException e) {
            throw new InventoryServiceUnavailableException("No se pudo iniciar sesión en inventory-service", e);
        }
    }

    private record LoginRequest(String username, String password) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LoginResponse(String accessToken, String tokenType, Instant expiresAt, List<String> roles) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ArticleSummary(UUID id, String sku, String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PointOfSaleSummary(UUID id, String code, String name) {
    }

    private record RegisterPosSaleRequest(UUID pointOfSaleId, UUID articleId, int quantity, String reason,
                                          String externalReference) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PosSaleResponse(String folio) {
    }
}
