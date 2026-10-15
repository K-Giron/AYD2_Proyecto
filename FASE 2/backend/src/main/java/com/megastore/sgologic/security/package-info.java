/**
 * Encapsula autenticación, autorización y controles de protección de datos.
 */
@org.springframework.modulith.ApplicationModule(
        id = "security",
        displayName = "Security",
        allowedDependencies = {"shared", "customers"}
)
package com.megastore.sgologic.security;
