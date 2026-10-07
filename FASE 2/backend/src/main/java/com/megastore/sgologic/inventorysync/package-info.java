/**
 * Sincroniza en el almacén central (inventory-service) la salida de
 * mercadería cuando una orden confirmada en la web representa una venta
 * real. Es la primera integración en vivo entre el monolito y ese
 * sistema, antes deliberadamente separados (ver ADR-015): el sentido es
 * siempre monolito → inventory-service, nunca al revés, y una caída de
 * inventory-service no bloquea la venta (reintentos con backoff y
 * bandeja de fallidos local).
 */
@org.springframework.modulith.ApplicationModule(
        id = "inventorysync",
        displayName = "Inventory Sync",
        allowedDependencies = {"shared", "messaging", "orders", "onlinestore"}
)
package com.megastore.sgologic.inventorysync;
