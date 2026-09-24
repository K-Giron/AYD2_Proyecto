/**
 * Contiene infraestructura técnica transversal mínima y ninguna regla de
 * negocio. Se declara {@code OPEN} porque el checklist de entrega exige que
 * todo módulo use el formato común de error y la propagación de
 * {@code X-Correlation-Id} ({@code shared.http}); sin este cambio, Spring
 * Modulith trata los subpaquetes como internos y ningún módulo de negocio
 * podría compilar contra ellos. Kevin Girón amplió esta declaración junto
 * con {@code ModularArchitectureTest}; pendiente de revisión de Jairo Gómez
 * (arquitecto integrador) antes de integrar a {@code develop}.
 */
@org.springframework.modulith.ApplicationModule(
        id = "shared",
        displayName = "Shared",
        type = org.springframework.modulith.ApplicationModule.Type.OPEN
)
package com.megastore.sgologic.shared;
