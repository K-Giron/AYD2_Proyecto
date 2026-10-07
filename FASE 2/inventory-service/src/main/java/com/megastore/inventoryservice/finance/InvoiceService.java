package com.megastore.inventoryservice.finance;

import com.megastore.inventoryservice.auditlog.AuditLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class InvoiceService {

    private final InvoiceRepository repository;
    private final AuditLogService auditLogService;
    private final Clock clock;

    public InvoiceService(InvoiceRepository repository, AuditLogService auditLogService, Clock clock) {
        this.repository = repository;
        this.auditLogService = auditLogService;
        this.clock = clock;
    }

    @Transactional
    public Invoice create(InvoiceType type, String counterparty, String description, BigDecimal amount,
                           String relatedFolio, LocalDate dueDate) {
        Invoice invoice = repository.save(
                Invoice.create(type, counterparty, description, amount, relatedFolio, dueDate, clock.instant()));
        auditLogService.record("CUENTA_REGISTRADA",
                (type == InvoiceType.PAYABLE ? "Cuenta por pagar a " : "Cuenta por cobrar a ") + counterparty
                        + " por Q" + amount + ".",
                relatedFolio, null);
        return invoice;
    }

    @Transactional
    public Invoice markPaid(UUID invoiceId) {
        Invoice invoice = repository.findById(invoiceId).orElseThrow(() -> new InvoiceNotFoundException(invoiceId));
        invoice.markPaid(clock.instant());
        Invoice saved = repository.save(invoice);
        auditLogService.record("CUENTA_PAGADA",
                "Se liquidó la cuenta de " + invoice.getCounterparty() + " por Q" + invoice.getAmount() + ".",
                invoice.getRelatedFolio(), null);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Invoice> list(InvoiceType type, InvoiceStatus status) {
        if (type != null) {
            return repository.findByTypeOrderByDueDateAsc(type);
        }
        if (status != null) {
            return repository.findByStatusOrderByDueDateAsc(status);
        }
        return repository.findAllByOrderByDueDateAsc();
    }
}
