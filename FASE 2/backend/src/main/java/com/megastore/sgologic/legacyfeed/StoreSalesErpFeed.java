package com.megastore.sgologic.legacyfeed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.megastore.sgologic.integration.ErpIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Las ventas de las tiendas físicas también llegan al ERP: cada minuto baja
 * del almacén central las facturas de tienda nuevas (las que cada tienda
 * subió desde su propio sistema) y las registra para el siguiente lote ERP,
 * con canal STORE, su punto de venta y su folio. Idempotente por el id de la
 * factura; si el central no responde, se reintenta en el siguiente ciclo.
 */
@Component
@ConditionalOnProperty(name = "sgo.integrations.erp.store-sales-feed-enabled", havingValue = "true", matchIfMissing = true)
class StoreSalesErpFeed {

    private static final Logger log = LoggerFactory.getLogger(StoreSalesErpFeed.class);

    private final ErpIntegrationService erpService;
    private final RestClient restClient;
    private final String username;
    private final String password;
    private volatile Instant cursor;
    private volatile boolean lastFailed;

    StoreSalesErpFeed(ErpIntegrationService erpService,
                      @Value("${sgo.integrations.inventory-service.base-url:http://inventory-service:8080}") String baseUrl,
                      @Value("${sgo.integrations.inventory-service.username:integracion-web@almacencentral.local}") String username,
                      @Value("${sgo.integrations.inventory-service.password:IntegracionWeb123}") String password,
                      @Value("${sgo.integrations.inventory-service.timeout-ms:5000}") long timeoutMs) {
        this.erpService = erpService;
        this.username = username;
        this.password = password;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) timeoutMs);
        requestFactory.setReadTimeout((int) timeoutMs);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    @Scheduled(initialDelayString = "${sgo.integrations.erp.store-sales-feed-initial-delay-ms:30000}",
            fixedDelayString = "${sgo.integrations.erp.store-sales-feed-interval-ms:60000}")
    void pull() {
        try {
            int registered = pullOnce();
            if (lastFailed || registered > 0) {
                log.info("Ventas de tienda física registradas para el ERP: {}.", registered);
            }
            lastFailed = false;
        } catch (RestClientException unavailable) {
            if (!lastFailed) {
                log.warn("No se pudieron bajar las facturas de tienda del almacén central: {}", unavailable.getMessage());
            }
            lastFailed = true;
        }
    }

    /** Baja y registra las facturas nuevas; devuelve cuántas se registraron. */
    int pullOnce() {
        LoginBody login = restClient.post().uri("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("username", username, "password", password)).retrieve().body(LoginBody.class);
        if (login == null || login.accessToken() == null) {
            throw new RestClientException("El almacén central no devolvió sesión.");
        }
        Instant since = cursor;
        StoreInvoice[] invoices = restClient.get()
                .uri(uri -> {
                    uri.path("/api/v1/store-sync/invoices");
                    if (since != null) {
                        uri.queryParam("since", since.toString());
                    }
                    return uri.build();
                })
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + login.accessToken())
                .retrieve().body(StoreInvoice[].class);
        List<StoreInvoice> list = invoices == null ? List.of() : Arrays.asList(invoices);
        int registered = 0;
        for (StoreInvoice invoice : list) {
            if (erpService.saleFor(invoice.id()).isPresent()) {
                continue;
            }
            BigDecimal[] split = ErpIntegrationService.splitIva(invoice.total());
            erpService.registerConfirmedSale(invoice.id(), split[0], split[1], invoice.total(), "GTQ",
                    invoice.issuedAt(), "STORE", invoice.pointOfSaleCode(),
                    invoice.centralFolio() + "/" + invoice.ticketFolio());
            registered++;
        }
        list.stream().map(StoreInvoice::receivedAt).max(Instant::compareTo).ifPresent(latest -> cursor = latest);
        return registered;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record LoginBody(String accessToken) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record StoreInvoice(UUID id, String centralFolio, String ticketFolio, String pointOfSaleCode, Instant issuedAt,
                        Instant receivedAt, BigDecimal total) {
    }
}
