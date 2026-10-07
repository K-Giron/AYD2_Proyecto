package com.megastore.sgologic.inventorysync;

import java.util.UUID;

/**
 * Puerto hacia el almacén central (inventory-service) — mismo estilo que
 * los demás puertos externos del monolito (ej. {@code LogiExpressGatewayPort}):
 * una sola implementación real hoy, sin simulador, porque esta
 * integración existe específicamente para reflejar ventas reales.
 */
public interface InventoryStockPort {

    /**
     * Registra en inventory-service una línea vendida en la web: descuenta
     * la existencia propia de "Tienda Online" (POS-ONLINE), que solo se
     * surte con folios del almacén central — igual que una tienda física.
     * Idempotente por {@code orderId + productId}: una llamada repetida
     * (reintento) devuelve el mismo folio en vez de descontar dos veces.
     *
     * @return el folio de venta del punto de venta (ej. {@code POS-VTA-000123}).
     * @throws InventoryArticleNotFoundException si el nombre del producto no tiene artículo correspondiente.
     * @throws InventoryStoreSaleRejectedException si POS-ONLINE no tiene esa existencia.
     * @throws InventoryServiceUnavailableException si la llamada falla por red, timeout o login.
     */
    String registerSale(SoldLine line, UUID orderId);

    /**
     * Registra en inventory-service una venta cobrada en una tienda física:
     * descuenta la existencia propia de ese punto de venta (no la del
     * almacén central). Idempotente por {@code orderId + productId}.
     *
     * @return el folio de venta del punto de venta (ej. {@code POS-VTA-000004}).
     * @throws InventoryArticleNotFoundException si el nombre del producto no tiene artículo correspondiente.
     * @throws InventoryStoreSaleRejectedException si la tienda no existe o no tiene esa existencia.
     * @throws InventoryServiceUnavailableException si la llamada falla por red, timeout o login.
     */
    String registerStoreSale(SoldLine line, UUID orderId, String pointOfSaleCode);
}
