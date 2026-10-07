/**
 * Alimenta automáticamente a los sistemas legados que el enunciado prohíbe
 * modificar (Kevin Girón, ADR-015):
 * <ul>
 *   <li>ERP AS/400: cada venta confirmada — web por evento de la orden, tienda
 *   física por las facturas que recibe el almacén central — queda lista para
 *   el siguiente lote CSV/XML (cada 30 min, configurable).</li>
 *   <li>POS de las 32 tiendas: cada cambio relevante de una orden se emite a
 *   todas las tiendas, con reintentos y tolerancia a fallos de red.</li>
 * </ul>
 * La entrega, los reintentos y la bandeja de fallidos son del módulo
 * {@code integration} (ERP-01/02, POS-01).
 */
@org.springframework.modulith.ApplicationModule(
        id = "legacyfeed",
        displayName = "Legacy Feed",
        allowedDependencies = {"shared", "messaging", "orders", "logistics", "integration"}
)
package com.megastore.sgologic.legacyfeed;
