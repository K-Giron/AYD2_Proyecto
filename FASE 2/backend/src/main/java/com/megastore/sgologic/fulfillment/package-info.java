/**
 * Cumplimiento (Kevin Girón, ADR-015): la orden sigue al despacho. Cuando
 * LogiExpress reporta que el pedido salió o se entregó (o la tienda de retiro
 * lo entrega al cliente), la orden pasa a {@code IN_TRANSIT} o
 * {@code DELIVERED} sola, sin que nadie la marque a mano en la consola.
 */
@org.springframework.modulith.ApplicationModule(
        id = "fulfillment",
        displayName = "Fulfillment",
        allowedDependencies = {"shared", "messaging", "orders"}
)
package com.megastore.sgologic.fulfillment;
