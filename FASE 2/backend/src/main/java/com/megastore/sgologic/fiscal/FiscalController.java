package com.megastore.sgologic.fiscal;

import com.megastore.sgologic.shared.http.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fiscal-documents")
public class FiscalController {

    private final FiscalService service;

    public FiscalController(FiscalService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<FiscalDocumentResponse> create(
            @Valid @RequestBody CreateFiscalDocumentRequest body,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            HttpServletRequest request) {
        UUID correlationId = UUID.fromString(request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME).toString());
        FiscalDocument document = service.requestDocument(
                body.orderId(), body.financialOperationId(), body.financialOperationType(),
                body.documentType(), body.originalDocumentId(),
                body.taxIdentifier(), body.customerName(), body.total(), body.currency(),
                correlationId, idempotencyKey);
        return ResponseEntity.accepted().body(FiscalDocumentResponse.from(document));
    }

    @GetMapping("/{documentId}")
    public FiscalDocumentResponse get(@PathVariable UUID documentId) {
        return FiscalDocumentResponse.from(service.getDocument(documentId));
    }

    @GetMapping("/orders/{orderId}")
    public List<FiscalDocumentResponse> getForOrder(@PathVariable UUID orderId) {
        return service.getDocumentsForOrder(orderId).stream().map(FiscalDocumentResponse::from).toList();
    }

    @GetMapping("/orders/{orderId}/confirmation")
    public FiscalConfirmationResponse confirmation(@PathVariable UUID orderId) {
        return new FiscalConfirmationResponse(orderId, service.canConfirmOrder(orderId));
    }

    public record CreateFiscalDocumentRequest(
            @NotNull UUID orderId,
            @NotNull UUID financialOperationId,
            @NotNull FinancialOperationType financialOperationType,
            UUID paymentAttemptId,
            @NotNull FiscalDocumentType documentType,
            UUID originalDocumentId,
            @NotBlank @Size(max = 32) String taxIdentifier,
            @NotBlank @Size(max = 200) String customerName,
            @NotNull @DecimalMin("0.01") BigDecimal total,
            @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency
    ) {
        public CreateFiscalDocumentRequest {
            // Compatibilidad hacia atrás: los consumidores de Fase 2 enviaban
            // únicamente paymentAttemptId. Internamente todo queda normalizado
            // al contrato neutral de operación financiera de Fase 3.
            if (financialOperationId == null && paymentAttemptId != null) {
                financialOperationId = paymentAttemptId;
            }
            if (financialOperationType == null && paymentAttemptId != null) {
                financialOperationType = FinancialOperationType.PAYKLIK;
            }
            if (paymentAttemptId == null && financialOperationType == FinancialOperationType.PAYKLIK) {
                paymentAttemptId = financialOperationId;
            }
        }
    }

    public record FiscalDocumentResponse(
            UUID id,
            UUID orderId,
            UUID paymentAttemptId,
            UUID financialOperationId,
            FinancialOperationType financialOperationType,
            FiscalDocumentType documentType,
            UUID originalDocumentId,
            String cui,
            String series,
            BigDecimal total,
            String currency,
            FiscalStatus status,
            String satAuthorization,
            String failureCode,
            String failureMessage,
            int submissionAttempts,
            UUID correlationId,
            Instant createdAt,
            Instant updatedAt,
            Instant confirmedAt,
            String xmlPayload
    ) {
        static FiscalDocumentResponse from(FiscalDocument document) {
            return new FiscalDocumentResponse(document.getId(), document.getOrderId(),
                    document.getPaymentAttemptId(), document.getFinancialOperationId(),
                    document.getFinancialOperationType(), document.getDocumentType(), document.getOriginalDocumentId(),
                    document.getCui(), document.getSeries(), document.getTotal(), document.getCurrency(),
                    document.getStatus(), document.getSatAuthorization(), document.getFailureCode(),
                    document.getFailureMessage(), document.getSubmissionAttempts(), document.getCorrelationId(),
                    document.getCreatedAt(), document.getUpdatedAt(), document.getConfirmedAt(),
                    document.getXmlPayload());
        }
    }

    public record FiscalConfirmationResponse(UUID orderId, boolean fiscallyConfirmed) {
    }
}
