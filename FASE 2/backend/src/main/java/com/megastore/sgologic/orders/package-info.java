/**
 * Gestiona el carrito omnicanal y el ciclo de vida de la orden (CUN-01).
 * El módulo conserva únicamente identificadores de agregados ajenos y se
 * integra con Inventario, Pagos, Fiscal y Logística mediante contratos, sin
 * acceder a sus tablas o repositorios.
 */
@org.springframework.modulith.ApplicationModule(
        id = "orders",
        displayName = "Orders",
        allowedDependencies = {"shared", "inventory", "payments", "fiscal", "messaging", "audit"}
)
package com.megastore.sgologic.orders;
