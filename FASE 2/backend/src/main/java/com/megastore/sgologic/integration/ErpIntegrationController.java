package com.megastore.sgologic.integration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/integrations/erp")
class ErpIntegrationController {

    private final ErpIntegrationService service;

    ErpIntegrationController(ErpIntegrationService service) {
        this.service = service;
    }

    @PostMapping("/sales")
    ResponseEntity<ErpSaleResponse> register(@Valid @RequestBody RegisterErpSaleRequest body) {
        ErpSale sale = service.registerConfirmedSale(body.orderId(), body.netTotal(), body.taxTotal(),
                body.grandTotal(), body.currency(), body.confirmedAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(ErpSaleResponse.from(sale));
    }

    @PostMapping("/batches")
    ResponseEntity<ErpBatchResponse> runBatch(@RequestParam(required = false) ErpExportFormat format) {
        return ResponseEntity.accepted().body(ErpBatchResponse.from(service.runBatch(format)));
    }

    @GetMapping("/batches/{batchId}")
    ErpBatchResponse getBatch(@PathVariable UUID batchId) {
        return ErpBatchResponse.from(service.getBatch(batchId));
    }

    record RegisterErpSaleRequest(
            @NotNull UUID orderId,
            @NotNull @DecimalMin("0.00") BigDecimal netTotal,
            @NotNull @DecimalMin("0.00") BigDecimal taxTotal,
            @NotNull @DecimalMin("0.01") BigDecimal grandTotal,
            @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
            @NotNull Instant confirmedAt
    ) {
    }

    record ErpSaleResponse(UUID id, UUID orderId, String status, UUID batchId, Instant confirmedAt) {
        static ErpSaleResponse from(ErpSale sale) {
            return new ErpSaleResponse(sale.getId(), sale.getOrderId(), sale.getStatus().name(),
                    sale.getBatchId(), sale.getConfirmedAt());
        }
    }

    record ErpBatchResponse(UUID id, ErpExportFormat format, ErpBatchStatus status, int saleCount,
                            int attempts, String acknowledgement, String failureMessage,
                            Instant createdAt, Instant deliveredAt, String payload) {
        static ErpBatchResponse from(ErpBatch batch) {
            return new ErpBatchResponse(batch.getId(), batch.getFormat(), batch.getStatus(),
                    batch.getSaleCount(), batch.getAttempts(), batch.getAcknowledgement(),
                    batch.getFailureMessage(), batch.getCreatedAt(), batch.getDeliveredAt(), batch.getPayload());
        }
    }
}
