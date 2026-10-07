package com.megastore.inventoryservice.overview;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@code quantities}: existencia por ubicación — "CENTRAL" para el almacén
 * central y el código del punto de venta para su almacén propio.
 */
public record StockOverviewResponse(List<Location> locations, List<ArticleRow> articles) {

    public static final String CENTRAL = "CENTRAL";

    /** {@code kind}: CENTRAL, ONLINE o STORE. */
    public record Location(String code, String name, String warehouseCode, String kind) {
    }

    public record ArticleRow(UUID articleId, String sku, String name, BigDecimal price, Map<String, Integer> quantities,
                             int total) {
    }
}
