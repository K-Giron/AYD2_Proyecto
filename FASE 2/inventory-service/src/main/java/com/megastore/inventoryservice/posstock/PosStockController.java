package com.megastore.inventoryservice.posstock;

import com.megastore.inventoryservice.article.Article;
import com.megastore.inventoryservice.article.ArticleService;
import com.megastore.inventoryservice.pointofsale.PointOfSaleResponse;
import com.megastore.inventoryservice.pointofsale.PointOfSaleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Existencia propia de cada punto de venta: sube solo con los folios de
 * salida del almacén central (ver {@code StockController}) y baja con las
 * ventas en tienda física ({@code POST /sales}).
 */
@RestController
@RequestMapping("/api/v1/pos-stock")
public class PosStockController {

    private final PosStockService posStockService;
    private final PointOfSaleService pointOfSaleService;
    private final ArticleService articleService;

    public PosStockController(PosStockService posStockService, PointOfSaleService pointOfSaleService,
                               ArticleService articleService) {
        this.posStockService = posStockService;
        this.pointOfSaleService = pointOfSaleService;
        this.articleService = articleService;
    }

    @GetMapping
    public List<PosStockResponse> listStock(@RequestParam(required = false) UUID pointOfSaleId) {
        return posStockService.listStock(pointOfSaleId).stream().map(this::toResponse).toList();
    }

    @GetMapping("/movements")
    public List<PosStockMovementResponse> listMovements(@RequestParam(required = false) UUID pointOfSaleId) {
        return posStockService.listMovements(pointOfSaleId).stream().map(this::toResponse).toList();
    }

    @PostMapping("/sales")
    public ResponseEntity<PosStockMovementResponse> registerSale(@Valid @RequestBody RegisterPosSaleRequest request) {
        pointOfSaleService.getResponseById(request.pointOfSaleId());
        PosStockMovement movement = posStockService.registerSale(request.pointOfSaleId(), request.articleId(),
                request.quantity(), request.reason(), request.externalReference());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(movement));
    }

    private PosStockResponse toResponse(PosStock stock) {
        PointOfSaleResponse pointOfSale = pointOfSaleService.getResponseById(stock.getPointOfSaleId());
        Article article = articleService.getById(stock.getArticleId());
        return new PosStockResponse(pointOfSale.id(), pointOfSale.code(), pointOfSale.name(),
                article.getId(), article.getSku(), article.getName(), stock.getQuantityOnHand(), stock.getUpdatedAt());
    }

    private PosStockMovementResponse toResponse(PosStockMovement movement) {
        PointOfSaleResponse pointOfSale = pointOfSaleService.getResponseById(movement.getPointOfSaleId());
        Article article = articleService.getById(movement.getArticleId());
        return new PosStockMovementResponse(movement.getId(), movement.getFolio(), movement.getType(),
                pointOfSale.id(), pointOfSale.code(), article.getId(), article.getSku(), article.getName(),
                movement.getQuantity(), movement.getOriginWarehouseFolio(), movement.getExternalReference(),
                movement.getOccurredAt());
    }
}
