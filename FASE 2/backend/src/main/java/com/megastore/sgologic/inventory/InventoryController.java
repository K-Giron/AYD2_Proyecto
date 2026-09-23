package com.megastore.sgologic.inventory;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Expone INV-01: catálogo de ubicaciones y consulta de disponibilidad por
 * producto y por ubicación. Endpoints bajo {@code /api/v1}, JSON
 * {@code camelCase}, conforme a {@code api-conventions.md}. Consulta de
 * solo lectura; la reserva atómica (INV-02) se expondrá en un endpoint
 * separado cuando esté lista.
 */
@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryQueryService queryService;

    public InventoryController(InventoryQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/locations")
    public List<InventoryLocationResponse> getLocations() {
        return queryService.getLocations().stream()
                .map(InventoryLocationResponse::from)
                .toList();
    }

    /**
     * Sin {@code locationId}: disponibilidad del producto en todas las
     * ubicaciones donde tiene stock registrado. Con {@code locationId}:
     * disponibilidad en esa ubicación específica.
     */
    @GetMapping("/stock")
    public List<InventoryStockResponse> getStock(
            @RequestParam UUID productId,
            @RequestParam(required = false) UUID locationId) {
        if (locationId != null) {
            return List.of(queryService.getStockByProductAndLocation(productId, locationId));
        }
        return queryService.getStockByProduct(productId);
    }
}
