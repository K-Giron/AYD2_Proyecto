package com.megastore.inventoryservice.overview;

import com.megastore.inventoryservice.article.Article;
import com.megastore.inventoryservice.article.ArticleService;
import com.megastore.inventoryservice.pointofsale.PointOfSaleResponse;
import com.megastore.inventoryservice.pointofsale.PointOfSaleService;
import com.megastore.inventoryservice.posstock.PosStock;
import com.megastore.inventoryservice.posstock.PosStockService;
import com.megastore.inventoryservice.stock.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Vista general de existencias y ubicaciones: por cada artículo, cuánto
 * hay en el almacén central y en el almacén de cada punto de venta
 * (incluida la tienda online), y el total de la red. Solo consulta.
 */
@Service
public class StockOverviewService {

    private final ArticleService articleService;
    private final StockService stockService;
    private final PointOfSaleService pointOfSaleService;
    private final PosStockService posStockService;

    public StockOverviewService(ArticleService articleService, StockService stockService,
                                PointOfSaleService pointOfSaleService, PosStockService posStockService) {
        this.articleService = articleService;
        this.stockService = stockService;
        this.pointOfSaleService = pointOfSaleService;
        this.posStockService = posStockService;
    }

    @Transactional(readOnly = true)
    public StockOverviewResponse overview() {
        List<PointOfSaleResponse> pointsOfSale = new ArrayList<>(pointOfSaleService.listAll());
        // Tienda online primero entre los puntos de venta; las tiendas físicas por código.
        pointsOfSale.sort(Comparator.comparing((PointOfSaleResponse pos) -> !"POS-ONLINE".equals(pos.code()))
                .thenComparing(PointOfSaleResponse::code));
        Map<UUID, String> codeById = new LinkedHashMap<>();
        pointsOfSale.forEach(pos -> codeById.put(pos.id(), pos.code()));

        List<StockOverviewResponse.Location> locations = new ArrayList<>();
        locations.add(new StockOverviewResponse.Location(StockOverviewResponse.CENTRAL, "Almacén central",
                "ALM-CENTRAL", "CENTRAL"));
        pointsOfSale.forEach(pos -> locations.add(new StockOverviewResponse.Location(pos.code(), pos.name(),
                pos.warehouseCode(), "POS-ONLINE".equals(pos.code()) ? "ONLINE" : "STORE")));

        Map<UUID, Map<String, Integer>> byArticle = new LinkedHashMap<>();
        stockService.listStock().forEach(balance -> byArticle
                .computeIfAbsent(balance.article().getId(), id -> new LinkedHashMap<>())
                .put(StockOverviewResponse.CENTRAL, balance.stock().getQuantityOnHand()));
        for (PosStock stock : posStockService.listStock(null)) {
            String code = codeById.get(stock.getPointOfSaleId());
            if (code != null) {
                byArticle.computeIfAbsent(stock.getArticleId(), id -> new LinkedHashMap<>())
                        .merge(code, stock.getQuantityOnHand(), Integer::sum);
            }
        }

        List<StockOverviewResponse.ArticleRow> rows = articleService.listAll().stream()
                .sorted(Comparator.comparing(Article::getName))
                .map(article -> {
                    Map<String, Integer> quantities = byArticle.getOrDefault(article.getId(), Map.of());
                    int total = quantities.values().stream().mapToInt(Integer::intValue).sum();
                    return new StockOverviewResponse.ArticleRow(article.getId(), article.getSku(), article.getName(),
                            article.getPrice(), quantities, total);
                })
                .toList();
        return new StockOverviewResponse(locations, rows);
    }
}
