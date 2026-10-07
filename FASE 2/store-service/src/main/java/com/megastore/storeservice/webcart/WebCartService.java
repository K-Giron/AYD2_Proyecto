package com.megastore.storeservice.webcart;

import com.megastore.storeservice.inventory.InventoryService;
import com.megastore.storeservice.inventory.StoreArticle;
import com.megastore.storeservice.shared.StoreBusinessException;
import com.megastore.storeservice.sync.SyncLog;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Cliente identificado (opcional): trae su carrito web y lo convierte en
 * líneas de esta tienda. El catálogo web y el de la tienda son
 * independientes; se cruzan por nombre exacto del producto, y se cobra al
 * precio de la tienda. Lo que la tienda no tiene se informa, no se vende.
 */
@Service
public class WebCartService {

    private final MonolithClient monolith;
    private final InventoryService inventoryService;
    private final SyncLog syncLog;

    public WebCartService(MonolithClient monolith, InventoryService inventoryService, SyncLog syncLog) {
        this.monolith = monolith;
        this.inventoryService = inventoryService;
        this.syncLog = syncLog;
    }

    public WebCartResponse fetch(String username) {
        MonolithClient.Customer customer = monolith.findCustomer(username.trim()).orElseThrow(() ->
                new StoreBusinessException(HttpStatus.NOT_FOUND, "CUSTOMER_NOT_FOUND",
                        "No hay ningún cliente con el usuario " + username + " en la tienda web."));
        Optional<MonolithClient.Cart> cart = monolith.activeCart(customer.id());
        List<WebCartResponse.Line> lines = new ArrayList<>();
        List<String> unavailable = new ArrayList<>();
        cart.ifPresent(active -> active.items().forEach(item -> {
            Optional<StoreArticle> article = inventoryService.findByName(item.productName());
            if (article.isEmpty()) {
                unavailable.add(item.productName() + " (no está en esta tienda)");
                return;
            }
            int onHand = inventoryService.quantityOf(article.get().getId());
            lines.add(new WebCartResponse.Line(article.get().getId(), article.get().getSku(), article.get().getName(),
                    article.get().getPrice(), item.quantity(), onHand, item.unitPrice()));
        }));
        syncLog.record(SyncLog.BAJADA, "CARRITO_WEB", customer.username(), true,
                cart.map(active -> "Carrito web de " + customer.fullName() + ": " + lines.size()
                                + " línea(s) disponibles en tienda" + (unavailable.isEmpty() ? "." : ", sin existencia: "
                                + String.join(", ", unavailable) + "."))
                        .orElse(customer.fullName() + " no tiene carrito web activo."));
        return new WebCartResponse(customer.username(), customer.fullName(), cart.map(MonolithClient.Cart::id).orElse(null),
                lines, unavailable);
    }

    /** Best effort: la venta ya está cobrada; si falla, queda en la bitácora para cerrarlo a mano. */
    public void closeAfterSale(UUID cartId, String saleFolio, String seller, String storeCode) {
        try {
            monolith.abandonCart(cartId, "seller:" + seller + "@" + storeCode);
            syncLog.record(SyncLog.SUBIDA, "CARRITO_WEB", saleFolio, true,
                    "Carrito web " + cartId + " cerrado: se cobró en tienda con el ticket " + saleFolio + ".");
        } catch (RuntimeException error) {
            syncLog.record(SyncLog.SUBIDA, "CARRITO_WEB", saleFolio, false,
                    "No se pudo cerrar el carrito web " + cartId + ": " + error.getMessage());
        }
    }

    public record WebCartResponse(String username, String fullName, UUID cartId, List<Line> lines,
                                  List<String> unavailable) {
        public record Line(UUID articleId, String sku, String name, BigDecimal price, int quantity, int onHand,
                           BigDecimal webPrice) {
        }
    }
}
