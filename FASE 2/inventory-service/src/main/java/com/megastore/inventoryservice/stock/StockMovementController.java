package com.megastore.inventoryservice.stock;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stock-movements")
public class StockMovementController {

    private final StockService stockService;

    public StockMovementController(StockService stockService) {
        this.stockService = stockService;
    }

    @PostMapping
    public ResponseEntity<StockMovementResponse> register(@Valid @RequestBody RegisterStockMovementRequest request,
                                                          Authentication authentication) {
        // Un ajuste baja existencia sin destino: solo Almacén, no Logística (que solo despacha a tiendas).
        if (request.type() == StockMovementType.AJUSTE && authentication.getAuthorities().stream()
                .noneMatch(authority -> "ROLE_ALMACEN".equals(authority.getAuthority()))) {
            throw new AccessDeniedException("Solo el rol Almacén puede registrar ajustes de existencia.");
        }
        StockMovement movement = stockService.registerMovement(
                request.articleId(), request.type(), request.quantity(), request.reason(), request.pointOfSaleId(),
                request.externalReference());
        int quantityOnHandAfter = stockService.getQuantityOnHand(movement.getArticleId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StockMovementResponse.from(movement, stockService.resolveArticle(movement.getArticleId()),
                        quantityOnHandAfter, stockService.resolvePointOfSale(movement.getPointOfSaleId()),
                        stockService.resolveWarehouse(movement.getWarehouseId())));
    }

    @GetMapping
    public List<StockMovementResponse> list(@RequestParam(required = false) UUID articleId) {
        return stockService.listMovements(articleId).stream()
                .map(movement -> StockMovementResponse.from(movement,
                        stockService.resolveArticle(movement.getArticleId()),
                        stockService.getQuantityOnHand(movement.getArticleId()),
                        stockService.resolvePointOfSale(movement.getPointOfSaleId()),
                        stockService.resolveWarehouse(movement.getWarehouseId())))
                .toList();
    }
}
