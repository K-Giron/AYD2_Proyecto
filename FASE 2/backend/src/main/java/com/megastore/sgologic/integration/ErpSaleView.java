package com.megastore.sgologic.integration;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Venta tal como la conoce el conector ERP, para otros módulos (informe de
 * ventas). {@code status}: PENDING (espera lote), BATCHED (en un lote aún no
 * entregado) o DELIVERED (el ERP acusó recibo del archivo).
 */
public record ErpSaleView(UUID saleId, String channel, String pointOfSale, String reference, BigDecimal netTotal,
                          BigDecimal taxTotal, BigDecimal grandTotal, String currency, Instant confirmedAt,
                          String status, UUID batchId, String batchStatus) {
}
