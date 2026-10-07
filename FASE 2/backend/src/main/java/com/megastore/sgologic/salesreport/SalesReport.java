package com.megastore.sgologic.salesreport;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Informe de ventas de un rango de fechas. Cada fila es una venta (orden web
 * o factura de tienda) con el estado de cada paso que debe cuadrar; las
 * {@code discrepancies} son las diferencias que Finanzas debe revisar.
 */
public record SalesReport(UUID reportId, LocalDate from, LocalDate to, Instant generatedAt, String generatedBy,
                         Summary summary, List<ChannelTotal> byChannel, List<Row> rows) {

    public record Summary(int sales, BigDecimal total, BigDecimal net, BigDecimal tax, BigDecimal deliveredToErp,
                          BigDecimal pendingErp, int discrepancies, boolean reconciled) {
    }

    public record ChannelTotal(String channel, String pointOfSale, int sales, BigDecimal total) {
    }

    /**
     * {@code paymentStatus}: APPROVED/REJECTED/… (web) o COBRADO_EN_TIENDA.
     * {@code fiscalStatus}: APPROVED/… con CUI y serie (web) o FACTURA_TIENDA.
     * {@code erpStatus}: SIN_REGISTRAR, PENDING, BATCHED o DELIVERED.
     */
    public record Row(UUID saleId, Instant date, String channel, String pointOfSale, String reference,
                      String orderStatus, BigDecimal total, String paymentStatus, String paymentReference,
                      String fiscalStatus, String fiscalDocument, String erpStatus, BigDecimal erpTotal,
                      String erpBatchStatus, List<String> discrepancies) {
    }
}
