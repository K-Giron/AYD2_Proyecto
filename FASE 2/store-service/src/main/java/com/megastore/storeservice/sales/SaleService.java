package com.megastore.storeservice.sales;

import com.megastore.storeservice.inventory.InventoryService;
import com.megastore.storeservice.inventory.StoreArticle;
import com.megastore.storeservice.shared.StoreBusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class SaleService {

    /** NIT guatemalteco: dígitos y un verificador (dígito o K), con o sin guion. */
    private static final Pattern NIT = Pattern.compile("^[0-9]{1,12}-?[0-9K]$");

    private final SaleRepository repository;
    private final InventoryService inventoryService;
    private final Clock clock;

    public SaleService(SaleRepository repository, InventoryService inventoryService, Clock clock) {
        this.repository = repository;
        this.inventoryService = inventoryService;
        this.clock = clock;
    }

    /**
     * Cobra una venta en la tienda: descuenta la existencia local y emite
     * el ticket. Si alguna línea no alcanza, no se registra nada. No
     * depende del central: la factura queda PENDIENTE hasta subirla.
     */
    @Transactional
    public Sale create(CreateSaleRequest request) {
        String taxId = normalizeTaxId(request.taxId());
        String customerName = request.customerName() == null || request.customerName().isBlank()
                ? null : request.customerName().trim();
        if (Sale.CONSUMIDOR_FINAL.equals(taxId)) {
            customerName = customerName == null ? "Consumidor Final" : customerName;
        } else if (customerName == null) {
            throw new StoreBusinessException(HttpStatus.BAD_REQUEST, "CUSTOMER_NAME_REQUIRED",
                    "Una factura con NIT necesita el nombre del cliente.");
        }

        // Una misma línea escaneada dos veces se cobra como una sola, con la cantidad sumada.
        Map<UUID, Integer> quantities = new LinkedHashMap<>();
        request.lines().forEach(line -> quantities.merge(line.articleId(), line.quantity(), Integer::sum));

        List<SaleLine> lines = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : quantities.entrySet()) {
            StoreArticle article = inventoryService.getArticle(entry.getKey());
            inventoryService.take(article, entry.getValue());
            lines.add(SaleLine.of(article.getId(), article.getSku(), article.getName(), article.getPrice(),
                    entry.getValue()));
        }
        BigDecimal total = lines.stream().map(SaleLine::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal cashReceived = null;
        if (request.paymentMethod() == PaymentMethod.EFECTIVO) {
            cashReceived = request.cashReceived() == null ? total : request.cashReceived();
            if (cashReceived.compareTo(total) < 0) {
                throw new StoreBusinessException(HttpStatus.BAD_REQUEST, "CASH_NOT_ENOUGH",
                        "El efectivo recibido (Q" + cashReceived + ") no cubre el total (Q" + total + ").");
            }
        }

        String folio = "TKT-" + String.format("%06d", repository.nextTicketSequence());
        Instant now = clock.instant();
        return repository.saveAndFlush(Sale.register(folio, request.seller().trim(), taxId, customerName,
                request.paymentMethod(), lines, cashReceived, blankToNull(request.webCustomer()),
                request.webCartId(), now));
    }

    @Transactional(readOnly = true)
    public Sale get(UUID saleId) {
        return repository.findById(saleId).orElseThrow(() -> new StoreBusinessException(
                HttpStatus.NOT_FOUND, "SALE_NOT_FOUND", "No existe la venta " + saleId + "."));
    }

    @Transactional(readOnly = true)
    public List<Sale> list() {
        return repository.findTop200ByOrderByCreatedAtDesc();
    }

    /**
     * Facturas entre dos fechas (inclusive, en hora de la tienda), con texto
     * opcional que busca en folio, NIT, nombre del cliente o vendedor.
     */
    @Transactional(readOnly = true)
    public List<Sale> search(java.time.LocalDate from, java.time.LocalDate to, String text, java.time.ZoneId zone) {
        if (to.isBefore(from)) {
            throw new StoreBusinessException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE",
                    "La fecha final no puede ser anterior a la inicial.");
        }
        List<Sale> sales = repository.findTop1000ByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                from.atStartOfDay(zone).toInstant(), to.plusDays(1).atStartOfDay(zone).toInstant());
        String needle = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) {
            return sales;
        }
        return sales.stream()
                .filter(sale -> sale.getFolio().toLowerCase(Locale.ROOT).contains(needle)
                        || sale.getTaxId().toLowerCase(Locale.ROOT).contains(needle)
                        || sale.getCustomerName().toLowerCase(Locale.ROOT).contains(needle)
                        || sale.getSeller().toLowerCase(Locale.ROOT).contains(needle)
                        || (sale.getCentralFolio() != null && sale.getCentralFolio().toLowerCase(Locale.ROOT).contains(needle)))
                .toList();
    }

    /** Cuenta la reimpresión: la copia sale marcada como tal. */
    @Transactional
    public Sale reprint(UUID saleId) {
        Sale sale = get(saleId);
        sale.reprinted(clock.instant());
        return sale;
    }

    @Transactional(readOnly = true)
    public List<Sale> pending() {
        return repository.findBySyncStatusOrderByCreatedAtAsc(SaleSyncStatus.PENDIENTE);
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return repository.countBySyncStatus(SaleSyncStatus.PENDIENTE) + repository.countBySyncStatus(SaleSyncStatus.ERROR);
    }

    @Transactional
    public void markSent(UUID saleId, String centralFolio) {
        Sale sale = get(saleId);
        sale.markSent(centralFolio, clock.instant());
    }

    @Transactional
    public void markAttemptFailed(UUID saleId, String error, boolean rejected) {
        get(saleId).markAttemptFailed(error, rejected);
    }

    /** Una factura rechazada por el central vuelve a la cola cuando alguien lo pide. */
    @Transactional
    public Sale retry(UUID saleId) {
        Sale sale = get(saleId);
        sale.retry();
        return sale;
    }

    static String normalizeTaxId(String taxId) {
        if (taxId == null || taxId.isBlank()) {
            return Sale.CONSUMIDOR_FINAL;
        }
        String normalized = taxId.trim().toUpperCase(Locale.ROOT).replace(" ", "");
        if (Sale.CONSUMIDOR_FINAL.equals(normalized) || "C/F".equals(normalized)) {
            return Sale.CONSUMIDOR_FINAL;
        }
        if (!NIT.matcher(normalized).matches()) {
            throw new StoreBusinessException(HttpStatus.BAD_REQUEST, "INVALID_TAX_ID",
                    "El NIT " + taxId + " no es válido. Use dígitos y verificador (ej. 1234567-8) o CF.");
        }
        return normalized;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
