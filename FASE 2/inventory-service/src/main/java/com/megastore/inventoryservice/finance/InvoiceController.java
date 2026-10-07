package com.megastore.inventoryservice.finance;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping
    public List<InvoiceResponse> list(@RequestParam(required = false) InvoiceType type,
                                       @RequestParam(required = false) InvoiceStatus status) {
        return invoiceService.list(type, status).stream().map(InvoiceResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<InvoiceResponse> create(@Valid @RequestBody CreateInvoiceRequest request) {
        Invoice invoice = invoiceService.create(request.type(), request.counterparty(), request.description(),
                request.amount(), request.relatedFolio(), request.dueDate());
        return ResponseEntity.status(HttpStatus.CREATED).body(InvoiceResponse.from(invoice));
    }

    @PatchMapping("/{id}/pay")
    public InvoiceResponse markPaid(@PathVariable UUID id) {
        return InvoiceResponse.from(invoiceService.markPaid(id));
    }
}
