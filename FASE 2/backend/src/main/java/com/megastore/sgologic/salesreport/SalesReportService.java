package com.megastore.sgologic.salesreport;

import com.megastore.sgologic.audit.api.AuditEntryRequest;
import com.megastore.sgologic.audit.api.AuditRecorder;
import com.megastore.sgologic.fiscal.FiscalProcessingPort;
import com.megastore.sgologic.fiscal.FiscalProcessingResult;
import com.megastore.sgologic.integration.ErpIntegrationService;
import com.megastore.sgologic.integration.ErpSaleView;
import com.megastore.sgologic.orders.CustomerOrder;
import com.megastore.sgologic.orders.OrderStatus;
import com.megastore.sgologic.orders.OrdersService;
import com.megastore.sgologic.payments.api.PaymentPort;
import com.megastore.sgologic.payments.api.PaymentResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Construye el informe de ventas conciliado. Una venta cuadra cuando la
 * orden está confirmada, su pago fue aprobado, su factura fue certificada
 * por SAT y el ERP recibió el mismo monto. Las ventas de tienda física ya
 * vienen cobradas y facturadas por la tienda; de ellas se concilia la
 * entrega al ERP.
 */
@Service
public class SalesReportService {

    static final Set<OrderStatus> SOLD = EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.PREPARING,
            OrderStatus.IN_TRANSIT, OrderStatus.DELIVERED);
    /** Órdenes que nunca llegaron a cobrarse no son ventas ni diferencias. */
    static final Set<OrderStatus> NOT_SALES = EnumSet.of(OrderStatus.CREATED, OrderStatus.INVENTORY_RESERVED,
            OrderStatus.PAYMENT_REJECTED);

    private final OrdersService ordersService;
    private final PaymentPort paymentPort;
    private final FiscalProcessingPort fiscalPort;
    private final ErpIntegrationService erpService;
    private final AuditRecorder auditRecorder;
    private final Clock clock;
    private final ZoneId zone = ZoneId.of("America/Guatemala");

    public SalesReportService(OrdersService ordersService, PaymentPort paymentPort, FiscalProcessingPort fiscalPort,
                              ErpIntegrationService erpService, AuditRecorder auditRecorder, Clock clock) {
        this.ordersService = ordersService;
        this.paymentPort = paymentPort;
        this.fiscalPort = fiscalPort;
        this.erpService = erpService;
        this.auditRecorder = auditRecorder;
        this.clock = clock;
    }

    /** Genera el informe y deja la consulta en la bitácora de auditoría ({@code action}: consulta o exportación). */
    @Transactional
    public SalesReport generate(LocalDate from, LocalDate to, String action) {
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la inicial.");
        }
        Instant start = from.atStartOfDay(zone).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(zone).toInstant();
        List<SalesReport.Row> rows = new ArrayList<>();

        for (CustomerOrder order : ordersService.getAllOrders()) {
            if (order.getCreatedAt().isBefore(start) || !order.getCreatedAt().isBefore(end)
                    || NOT_SALES.contains(order.getStatus())) {
                continue;
            }
            rows.add(webRow(order));
        }
        for (ErpSaleView sale : erpService.salesConfirmedBetween(start, end)) {
            if ("STORE".equals(sale.channel())) {
                rows.add(storeRow(sale));
            }
        }
        rows.sort(Comparator.comparing(SalesReport.Row::date));

        List<SalesReport.Row> sold = rows.stream().filter(row -> !row.orderStatus().equals("NO_CONFIRMADA")).toList();
        BigDecimal total = sum(sold.stream().map(SalesReport.Row::total).toList());
        BigDecimal[] split = ErpIntegrationService.splitIva(total);
        BigDecimal delivered = sum(sold.stream().filter(row -> "DELIVERED".equals(row.erpStatus()))
                .map(SalesReport.Row::erpTotal).toList());
        BigDecimal pending = sum(sold.stream().filter(row -> "PENDING".equals(row.erpStatus()) || "BATCHED".equals(row.erpStatus()))
                .map(SalesReport.Row::erpTotal).toList());
        int discrepancies = (int) rows.stream().filter(row -> !row.discrepancies().isEmpty()).count();

        Map<String, SalesReport.ChannelTotal> byChannel = new LinkedHashMap<>();
        for (SalesReport.Row row : sold) {
            String key = row.channel() + "|" + row.pointOfSale();
            SalesReport.ChannelTotal current = byChannel.get(key);
            byChannel.put(key, new SalesReport.ChannelTotal(row.channel(), row.pointOfSale(),
                    (current == null ? 0 : current.sales()) + 1,
                    (current == null ? BigDecimal.ZERO : current.total()).add(row.total())));
        }

        UUID reportId = UUID.randomUUID();
        String actor = currentUser();
        SalesReport report = new SalesReport(reportId, from, to, clock.instant(), actor,
                new SalesReport.Summary(sold.size(), total, split[0], split[1], delivered, pending, discrepancies,
                        discrepancies == 0),
                List.copyOf(byChannel.values()), rows);
        auditRecorder.record(new AuditEntryRequest("SALES_REPORT", reportId, null, action, "SUCCESS", null,
                "{\"from\":\"" + from + "\",\"to\":\"" + to + "\",\"sales\":" + sold.size() + ",\"total\":\""
                        + total.toPlainString() + "\",\"discrepancies\":" + discrepancies + "}",
                reportId, null, null));
        return report;
    }

    private SalesReport.Row webRow(CustomerOrder order) {
        boolean sold = SOLD.contains(order.getStatus());
        Optional<PaymentResult> payment = paymentPort.latestForOrder(order.getId());
        Optional<FiscalProcessingResult> fiscal = fiscalPort.latestForOrder(order.getId());
        Optional<ErpSaleView> erp = erpService.saleFor(order.getId());
        String paymentStatus = payment.map(PaymentResult::status).orElse("SIN_PAGO");
        String fiscalStatus = fiscal.map(result -> result.status().name()).orElse("SIN_FACTURA");

        List<String> issues = new ArrayList<>();
        if (sold && !"APPROVED".equals(paymentStatus)) {
            issues.add("Venta confirmada sin pago aprobado");
        }
        if (sold && !"APPROVED".equals(fiscalStatus)) {
            issues.add("Venta confirmada sin factura certificada por SAT");
        }
        if (!sold && "APPROVED".equals(paymentStatus)) {
            issues.add("Pago aprobado sin venta confirmada (" + order.getStatus() + "): revisar o reembolsar");
        }
        if (sold && erp.isEmpty()) {
            issues.add("No registrada en el ERP");
        }
        erp.ifPresent(sale -> erpIssues(sale, order.getTotal(), issues));

        return new SalesReport.Row(order.getId(), order.getCreatedAt(), order.getChannel().name(), "POS-ONLINE",
                order.getId().toString(), sold ? order.getStatus().name() : "NO_CONFIRMADA", order.getTotal(),
                paymentStatus, payment.map(PaymentResult::providerReference).orElse(null), fiscalStatus,
                fiscal.map(result -> result.series() + " " + result.cui()).orElse(null),
                erp.map(ErpSaleView::status).orElse("SIN_REGISTRAR"), erp.map(ErpSaleView::grandTotal).orElse(null),
                erp.map(ErpSaleView::batchStatus).orElse(null), issues);
    }

    private SalesReport.Row storeRow(ErpSaleView sale) {
        List<String> issues = new ArrayList<>();
        erpIssues(sale, sale.grandTotal(), issues);
        return new SalesReport.Row(sale.saleId(), sale.confirmedAt(), "STORE", sale.pointOfSale(), sale.reference(),
                "VENDIDA_EN_TIENDA", sale.grandTotal(), "COBRADO_EN_TIENDA", null, "FACTURA_TIENDA", sale.reference(),
                sale.status(), sale.grandTotal(), sale.batchStatus(), issues);
    }

    private static void erpIssues(ErpSaleView sale, BigDecimal expectedTotal, List<String> issues) {
        if (sale.grandTotal().compareTo(expectedTotal) != 0) {
            issues.add("Monto en ERP (" + sale.grandTotal() + ") distinto al de la venta (" + expectedTotal + ")");
        }
        if ("DEAD_LETTER".equals(sale.batchStatus()) || "REJECTED".equals(sale.batchStatus())) {
            issues.add("El lote ERP no fue aceptado (" + sale.batchStatus() + ")");
        }
    }

    private static BigDecimal sum(List<BigDecimal> values) {
        return values.stream().filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "sistema" : authentication.getName();
    }
}
