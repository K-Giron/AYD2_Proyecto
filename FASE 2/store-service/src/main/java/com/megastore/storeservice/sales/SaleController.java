package com.megastore.storeservice.sales;

import com.megastore.storeservice.shared.StoreProperties;
import com.megastore.storeservice.sync.SyncService;
import com.megastore.storeservice.webcart.WebCartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/store/sales")
public class SaleController {

    private final SaleService saleService;
    private final SyncService syncService;
    private final WebCartService webCartService;
    private final StoreProperties properties;

    public SaleController(SaleService saleService, SyncService syncService, WebCartService webCartService,
                         StoreProperties properties) {
        this.saleService = saleService;
        this.syncService = syncService;
        this.webCartService = webCartService;
        this.properties = properties;
    }

    /**
     * La venta se confirma con el commit local; después (fuera de esa
     * transacción) se intenta subir la factura y, si venía de la web,
     * cerrar el carrito. Ninguna de las dos cosas puede deshacer la venta.
     */
    @PostMapping
    public ResponseEntity<Sale> create(@Valid @RequestBody CreateSaleRequest request) {
        Sale sale = saleService.create(request);
        if (sale.getWebCartId() != null) {
            webCartService.closeAfterSale(sale.getWebCartId(), sale.getFolio(), sale.getSeller(), properties.code());
        }
        syncService.pushInvoice(sale.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(saleService.get(sale.getId()));
    }

    /** Sin fechas: las de hoy (hora de la tienda). */
    @GetMapping
    public List<Sale> list(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                           @RequestParam(required = false) String q) {
        ZoneId zone = ZoneId.of(properties.invoice().timezone());
        LocalDate today = LocalDate.now(zone);
        LocalDate start = from == null ? (to == null ? today : to) : from;
        LocalDate end = to == null ? (from == null ? today : from) : to;
        return saleService.search(start, end, q, zone);
    }

    @GetMapping("/{saleId}")
    public Sale get(@PathVariable UUID saleId) {
        return saleService.get(saleId);
    }

    @PostMapping("/{saleId}/reprint")
    public Sale reprint(@PathVariable UUID saleId) {
        return saleService.reprint(saleId);
    }

    @PostMapping("/{saleId}/retry")
    public Sale retry(@PathVariable UUID saleId) {
        saleService.retry(saleId);
        syncService.pushInvoice(saleId);
        return saleService.get(saleId);
    }
}
