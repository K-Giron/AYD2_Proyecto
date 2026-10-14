/**
 * Evalúa elegibilidad y concede el crédito propio de MegaStore sin depender
 * de PayKlik. Los demás módulos sólo consumen el contrato {@code credit-api}.
 */
@org.springframework.modulith.ApplicationModule(
        id = "credit",
        displayName = "Own Credit",
        allowedDependencies = {"shared", "audit :: audit-api", "integration :: outbox", "messaging"}
)
package com.megastore.sgologic.credit;
