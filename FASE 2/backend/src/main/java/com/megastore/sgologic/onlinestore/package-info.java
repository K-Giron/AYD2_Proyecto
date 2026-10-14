/**
 * Tienda online como punto de venta (POS-ONLINE del almacén central, ver
 * ADR-015): recibe mercadería por folio, la ingresa al inventario del
 * monolito en su propia ubicación (CD-WEB) para que la reserva y la Saga
 * operen sobre ese stock, y publica en la web solo lo que su administrador
 * decide (imagen, precio, descripción, categoría).
 */
@org.springframework.modulith.ApplicationModule(
        id = "onlinestore",
        displayName = "Online Store",
        allowedDependencies = {"shared", "inventory"}
)
package com.megastore.sgologic.onlinestore;
