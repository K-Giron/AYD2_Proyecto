/**
 * Directorio de clientes en el servidor (CDU-01: "el cliente está
 * identificado o puede identificarse en el canal"). Fija un
 * {@code customerId} único por usuario para que el carrito, que
 * {@code orders} ya conserva por {@code customerId}, sea el mismo desde la
 * web, desde otro dispositivo o desde la tienda física.
 */
@org.springframework.modulith.ApplicationModule(
        id = "customers",
        displayName = "Customers",
        allowedDependencies = {"shared"}
)
package com.megastore.sgologic.customers;
