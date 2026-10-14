/**
 * Informe de ventas conciliado y auditable (Finanzas): cruza cada venta con
 * su pago, su factura SAT y su envío al ERP AS/400, por canal (web y tiendas
 * físicas), y marca las diferencias. Cada consulta y exportación queda en la
 * bitácora de auditoría. Solo lectura sobre los demás módulos.
 */
@org.springframework.modulith.ApplicationModule(
        id = "salesreport",
        displayName = "Sales Report",
        allowedDependencies = {"shared", "orders", "payments :: payments-api", "credit :: credit-api", "fiscal", "integration",
                "audit :: audit-api"}
)
package com.megastore.sgologic.salesreport;
