package com.megastore.sgologic.salesreport;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.stream.Collectors;

/** Informe de ventas conciliado (solo Finanzas). Sin fechas: el día de hoy. */
@RestController
@RequestMapping("/api/v1/reports/sales")
class SalesReportController {

    private final SalesReportService service;

    SalesReportController(SalesReportService service) {
        this.service = service;
    }

    @GetMapping
    public SalesReport report(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate today = LocalDate.now(ZoneId.of("America/Guatemala"));
        return service.generate(from == null ? today : from, to == null ? today : to, "SALES_REPORT_VIEWED");
    }

    /** El mismo informe en CSV; la exportación queda auditada aparte. */
    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<String> export(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate today = LocalDate.now(ZoneId.of("America/Guatemala"));
        from = from == null ? today : from;
        to = to == null ? today : to;
        SalesReport report = service.generate(from, to, "SALES_REPORT_EXPORTED");
        String header = "fecha,canal,puntoVenta,referencia,estadoVenta,total,rutaFinanciera,estadoFinanciero,referenciaFinanciera,factura,documentoFiscal,erp,totalErp,loteErp,diferencias";
        String body = report.rows().stream().map(row -> String.join(",",
                        row.date().toString(), row.channel(), csv(row.pointOfSale()), csv(row.reference()), row.orderStatus(),
                        row.total().toPlainString(), row.financialRoute(), row.paymentStatus(),
                        csv(row.paymentReference()), row.fiscalStatus(),
                        csv(row.fiscalDocument()), row.erpStatus(), row.erpTotal() == null ? "" : row.erpTotal().toPlainString(),
                        csv(row.erpBatchStatus()), csv(String.join(" | ", row.discrepancies()))))
                .collect(Collectors.joining("\n"));
        String trailer = "\nTOTAL,,,,," + report.summary().total().toPlainString() + ",,,,,,,"
                + report.summary().deliveredToErp().toPlainString() + ",,diferencias=" + report.summary().discrepancies()
                + "\nGENERADO," + report.generatedAt() + "," + csv(report.generatedBy()) + ",informe=" + report.reportId();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"informe-ventas_" + from + "_" + to + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(header + "\n" + body + trailer);
    }

    private static String csv(String value) {
        return value == null ? "" : value.replace(",", " ").replace("\n", " ");
    }
}
