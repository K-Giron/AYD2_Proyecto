package com.megastore.sgologic.fiscal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * "Consultar factura" para el cliente (CDU de facturación): la factura de
 * su orden con lo que necesita ver e imprimir — CUI, serie, autorización
 * SAT, NIT, nombre, total y el XML emitido —, sin los datos internos del
 * documento fiscal (intento de pago, códigos de falla, correlación) que
 * {@code /api/v1/fiscal-documents} reserva a Finanzas (ADR-013).
 */
@RestController
@RequestMapping("/api/v1/invoices")
class CustomerInvoiceController {

    private final FiscalService service;

    CustomerInvoiceController(FiscalService service) {
        this.service = service;
    }

    /** La factura más reciente de la orden; 404 si aún no se emitió. */
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<CustomerInvoiceResponse> forOrder(@PathVariable UUID orderId) {
        return service.getDocumentsForOrder(orderId).stream()
                .filter(document -> document.getDocumentType() == FiscalDocumentType.INVOICE)
                .findFirst()
                .map(CustomerInvoiceResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    record CustomerInvoiceResponse(UUID orderId, String cui, String series, FiscalStatus status,
                                   String satAuthorization, String taxIdentifier, String customerName,
                                   BigDecimal total, String currency, Instant issuedAt, Instant certifiedAt,
                                   String xml) {
        static CustomerInvoiceResponse from(FiscalDocument document) {
            return new CustomerInvoiceResponse(document.getOrderId(), document.getCui(), document.getSeries(),
                    document.getStatus(), document.getSatAuthorization(), document.getTaxIdentifier(),
                    document.getCustomerName(), document.getTotal(), document.getCurrency(), document.getCreatedAt(),
                    document.getConfirmedAt(), document.getXmlPayload());
        }
    }
}
