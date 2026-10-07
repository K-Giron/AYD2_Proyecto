package com.megastore.inventoryservice.overview;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Vista general de almacenes y existencias de toda la red (solo consulta, cualquier rol). */
@RestController
@RequestMapping("/api/v1/stock-overview")
public class StockOverviewController {

    private final StockOverviewService service;

    public StockOverviewController(StockOverviewService service) {
        this.service = service;
    }

    @GetMapping
    public StockOverviewResponse overview() {
        return service.overview();
    }
}
