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
import java.util.List;
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
                body.grandTotal(), body.currency(), body.confirmedAt(), "WEB", "POS-ONLINE",
                body.orderId().toString(), body.financialRoute());
        return ResponseEntity.status(HttpStatus.CREATED).body(ErpSaleResponse.from(sale));
    }

    @PostMapping("/batches")
    ResponseEntity<ErpBatchResponse> runBatch(@RequestParam(required = false) ErpExportFormat format) {
        return ResponseEntity.accepted().body(ErpBatchResponse.from(service.runBatch(format)));
    }

    /** La venta de una orden (o factura de tienda) tal como la tiene el conector ERP. */
    @GetMapping("/sales/{orderId}")
    ResponseEntity<ErpSaleView> sale(@PathVariable UUID orderId) {
        return service.saleFor(orderId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Últimos lotes (más recientes primero), para el seguimiento en la consola. */
    @GetMapping("/batches")
    List<ErpBatchResponse> recentBatches() {
        return service.recentBatches().stream().map(ErpBatchResponse::from).toList();
    }

    /** El archivo tal como se entregó al ERP (CSV o XML), para descargarlo y revisarlo. */
    @GetMapping("/batches/{batchId}/file")
    ResponseEntity<String> batchFile(@PathVariable UUID batchId) {
        ErpBatch batch = service.getBatch(batchId);
        if (batch.getPayload() == null) {
            return ResponseEntity.noContent().build();
        }
        String ack = batch.getAcknowledgement();
        String fileName = ack != null && ack.startsWith("ERP-FILE:") ? ack.substring(9)
                : "ventas_" + batchId.toString().substring(0, 8) + (batch.getFormat() == ErpExportFormat.CSV ? ".csv" : ".xml");
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(batch.getFormat() == ErpExportFormat.CSV
                        ? org.springframework.http.MediaType.parseMediaType("text/csv; charset=UTF-8")
                        : org.springframework.http.MediaType.APPLICATION_XML)
                .body(batch.getPayload());
    }

    /** Reenvía un lote en DEAD_LETTER (por ejemplo, después de una caída larga del ERP). */
    @PostMapping("/batches/{batchId}/retry")
    ResponseEntity<ErpBatchResponse> retryBatch(@PathVariable UUID batchId) {
        return ResponseEntity.accepted().body(ErpBatchResponse.from(service.retryBatch(batchId)));
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
            @NotNull Instant confirmedAt,
            String financialRoute
    ) {
        RegisterErpSaleRequest {
            if (financialRoute == null || financialRoute.isBlank()) financialRoute = "PAYKLIK";
        }
    }

    record ErpSaleResponse(UUID id, UUID orderId, String financialRoute, String status,
                           UUID batchId, Instant confirmedAt) {
        static ErpSaleResponse from(ErpSale sale) {
            return new ErpSaleResponse(sale.getId(), sale.getOrderId(), sale.getFinancialRoute(), sale.getStatus().name(),
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
