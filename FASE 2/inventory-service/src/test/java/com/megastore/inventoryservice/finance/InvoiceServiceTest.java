package com.megastore.inventoryservice.finance;

import com.megastore.inventoryservice.auditlog.AuditLogService;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InvoiceServiceTest {

    private final InvoiceRepository repository = mock(InvoiceRepository.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-01T10:00:00Z"), ZoneOffset.UTC);
    private final InvoiceService service = new InvoiceService(repository, auditLogService, clock);

    @Test
    void createsPendingPayableInvoice() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Invoice invoice = service.create(InvoiceType.PAYABLE, "Proveedor Textil SA", "Recepción de mercadería",
                new BigDecimal("1500.00"), "ENT-000001", LocalDate.parse("2026-10-15"));

        assertThat(invoice.getType()).isEqualTo(InvoiceType.PAYABLE);
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PENDING);
        assertThat(invoice.getRelatedFolio()).isEqualTo("ENT-000001");
        assertThat(invoice.getPaidAt()).isNull();
    }

    @Test
    void markPaidSetsStatusAndPaidAt() {
        Invoice invoice = Invoice.create(InvoiceType.RECEIVABLE, "Tienda 5", null, new BigDecimal("800.00"),
                "SAL-000001", LocalDate.parse("2026-10-20"), clock.instant());
        when(repository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Invoice paid = service.markPaid(invoice.getId());

        assertThat(paid.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(paid.getPaidAt()).isEqualTo(clock.instant());
    }

    @Test
    void markPaidThrowsWhenAlreadyPaid() {
        Invoice invoice = Invoice.create(InvoiceType.PAYABLE, "Proveedor", null, BigDecimal.TEN,
                null, LocalDate.parse("2026-10-20"), clock.instant());
        invoice.markPaid(clock.instant());
        when(repository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> service.markPaid(invoice.getId()))
                .isInstanceOf(InvoiceAlreadyPaidException.class);
    }

    @Test
    void markPaidThrowsWhenInvoiceDoesNotExist() {
        UUID invoiceId = UUID.randomUUID();
        when(repository.findById(invoiceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markPaid(invoiceId)).isInstanceOf(InvoiceNotFoundException.class);
    }

    @Test
    void listFiltersByTypeWhenProvided() {
        Invoice invoice = Invoice.create(InvoiceType.PAYABLE, "Proveedor", null, BigDecimal.TEN,
                null, LocalDate.parse("2026-10-20"), clock.instant());
        when(repository.findByTypeOrderByDueDateAsc(InvoiceType.PAYABLE)).thenReturn(List.of(invoice));

        assertThat(service.list(InvoiceType.PAYABLE, null)).containsExactly(invoice);
    }
}
