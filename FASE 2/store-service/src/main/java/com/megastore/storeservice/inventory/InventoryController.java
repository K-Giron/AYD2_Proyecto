package com.megastore.storeservice.inventory;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/store")
public class InventoryController {

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @GetMapping("/stock")
    public List<StockItemResponse> stock() {
        return service.listStock();
    }

    /** Lo que hace el escáner: código de barras = SKU del central. */
    @GetMapping("/articles/by-sku/{sku}")
    public StockItemResponse bySku(@PathVariable String sku) {
        return service.findBySku(sku);
    }

    /** Búsqueda manual en caja, cuando no hay código que escanear. */
    @GetMapping("/articles/search")
    public List<StockItemResponse> search(@RequestParam("q") String text) {
        return service.search(text);
    }

    @GetMapping("/receipts")
    public List<StoreReceipt> receipts() {
        return service.listReceipts();
    }
}
