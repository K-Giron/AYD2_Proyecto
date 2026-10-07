package com.megastore.sgologic.salesreport;

import com.megastore.sgologic.audit.api.AuditRecorder;
import com.megastore.sgologic.fiscal.FiscalProcessingPort;
import com.megastore.sgologic.fiscal.FiscalProcessingResult;
import com.megastore.sgologic.fiscal.FiscalStatus;
import com.megastore.sgologic.integration.ErpIntegrationService;
import com.megastore.sgologic.integration.ErpSaleView;
import com.megastore.sgologic.orders.CustomerOrder;
import com.megastore.sgologic.orders.OrderStatus;
import com.megastore.sgologic.orders.OrdersService;
import com.megastore.sgologic.orders.SalesChannel;
import com.megastore.sgologic.payments.api.PaymentPort;
import com.megastore.sgologic.payments.api.PaymentResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SalesReportServiceTest {

    private final OrdersService orders = mock(OrdersService.class);
    private final PaymentPort payments = mock(PaymentPort.class);
    private final FiscalProcessingPort fiscal = mock(FiscalProcessingPort.class);
    private final ErpIntegrationService erp = mock(ErpIntegrationService.class);
    private final AuditRecorder audit = mock(AuditRecorder.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-06T22:00:00Z"), ZoneOffset.UTC);
    private final SalesReportService service = new SalesReportService(orders, payments, fiscal, erp, audit, clock);
    private final Instant during = Instant.parse("2026-10-06T18:00:00Z");

    private CustomerOrder order(UUID id, OrderStatus status, String total) {
        CustomerOrder order = mock(CustomerOrder.class);
        when(order.getId()).thenReturn(id);
        when(order.getStatus()).thenReturn(status);
        when(order.getTotal()).thenReturn(new BigDecimal(total));
        when(order.getChannel()).thenReturn(SalesChannel.WEB);
        when(order.getCreatedAt()).thenReturn(during);
        return order;
    }

    private void paid(UUID id, String paymentStatus, FiscalStatus fiscalStatus) {
        when(payments.latestForOrder(id)).thenReturn(Optional.of(new PaymentResult(UUID.randomUUID(), paymentStatus, "PK-1", during)));
        when(fiscal.latestForOrder(id)).thenReturn(Optional.of(new FiscalProcessingResult(UUID.randomUUID(), id,
                fiscalStatus, "CUI-1", "SGO-2026", "SAT-1", null)));
    }

    private ErpSaleView erpSale(UUID id, String channel, String pos, String total, String status, String batchStatus) {
        return new ErpSaleView(id, channel, pos, channel.equals("STORE") ? "FT-000001/TKT-000001" : id.toString(),
                new BigDecimal(total).divide(new BigDecimal("1.12"), 2, java.math.RoundingMode.HALF_UP),
                BigDecimal.ZERO, new BigDecimal(total), "GTQ", during, status, null, batchStatus);
    }

    @Test
    void reconciledWebAndStoreSalesHaveNoDiscrepancies() {
        UUID web = UUID.randomUUID();
        UUID store = UUID.randomUUID();
        CustomerOrder webOrder = order(web, OrderStatus.PREPARING, "749.00");
        when(orders.getAllOrders()).thenReturn(List.of(webOrder));
        paid(web, "APPROVED", FiscalStatus.APPROVED);
        when(erp.saleFor(web)).thenReturn(Optional.of(erpSale(web, "WEB", "POS-ONLINE", "749.00", "DELIVERED", "DELIVERED")));
        when(erp.salesConfirmedBetween(any(), any())).thenReturn(List.of(
                erpSale(web, "WEB", "POS-ONLINE", "749.00", "DELIVERED", "DELIVERED"),
                erpSale(store, "STORE", "POS-01", "1520.00", "PENDING", null)));

        SalesReport report = service.generate(LocalDate.of(2026, 10, 6), LocalDate.of(2026, 10, 6), "SALES_REPORT_VIEWED");

        assertThat(report.rows()).hasSize(2);
        assertThat(report.summary().sales()).isEqualTo(2);
        assertThat(report.summary().total()).isEqualByComparingTo("2269.00");
        assertThat(report.summary().deliveredToErp()).isEqualByComparingTo("749.00");
        assertThat(report.summary().pendingErp()).isEqualByComparingTo("1520.00");
        assertThat(report.summary().reconciled()).isTrue();
        assertThat(report.byChannel()).extracting(SalesReport.ChannelTotal::pointOfSale).containsExactly("POS-ONLINE", "POS-01");
        verify(audit).record(any());
    }

    @Test
    void flagsTheDifferencesFinanceMustReview() {
        UUID notInErp = UUID.randomUUID();
        UUID paidNotConfirmed = UUID.randomUUID();
        CustomerOrder confirmed = order(notInErp, OrderStatus.CONFIRMED, "100.00");
        CustomerOrder rejected = order(paidNotConfirmed, OrderStatus.FISCAL_REJECTED, "50.00");
        when(orders.getAllOrders()).thenReturn(List.of(confirmed, rejected));
        paid(notInErp, "APPROVED", FiscalStatus.APPROVED);
        paid(paidNotConfirmed, "APPROVED", FiscalStatus.REJECTED);
        when(erp.saleFor(any())).thenReturn(Optional.empty());
        when(erp.salesConfirmedBetween(any(), any())).thenReturn(List.of());

        SalesReport report = service.generate(LocalDate.of(2026, 10, 6), LocalDate.of(2026, 10, 6), "SALES_REPORT_VIEWED");

        assertThat(report.summary().reconciled()).isFalse();
        assertThat(report.summary().discrepancies()).isEqualTo(2);
        assertThat(report.rows()).flatExtracting(SalesReport.Row::discrepancies)
                .anyMatch(text -> text.contains("No registrada en el ERP"))
                .anyMatch(text -> text.contains("Pago aprobado sin venta confirmada"));
        // La orden no confirmada no suma al total vendido.
        assertThat(report.summary().total()).isEqualByComparingTo("100.00");
    }
}
