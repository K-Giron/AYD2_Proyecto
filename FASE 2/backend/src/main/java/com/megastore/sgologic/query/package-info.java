/**
 * Mantiene modelos de lectura consolidados alimentados mediante contratos públicos o eventos.
 */
@org.springframework.modulith.ApplicationModule(
        id = "query",
        displayName = "Query",
        allowedDependencies = {"shared", "messaging"}
)
package com.megastore.sgologic.query;
