package com.megastore.sgologic;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ModularArchitectureTest {

    private static final Set<String> EXPECTED_MODULES = Set.of(
            "orders",
            "inventory",
            "payments",
            "fiscal",
            "logistics",
            "notifications",
            "incidents",
            "query",
            "audit",
            "security",
            "integration",
            "shared"
    );

    @Test
    void verifiesDeclaredModuleBoundaries() {
        ApplicationModules.of(SgoLogicApplication.class).verify();
    }

    @Test
    void declaresExactlyTheAgreedDirectModules() {
        ApplicationModules modules = ApplicationModules.of(SgoLogicApplication.class);
        Set<String> identifiers = modules.stream()
                .map(module -> module.getIdentifier().toString())
                .collect(Collectors.toSet());

        assertThat(identifiers).containsExactlyInAnyOrderElementsOf(EXPECTED_MODULES);
    }

    /**
     * {@code shared} concentra la infraestructura HTTP transversal que el
     * checklist de entrega exige a todo módulo (formato común de error,
     * propagación de {@code X-Correlation-Id}): ver
     * {@code FASE 2/docs/integration/module-delivery-checklist.md}. Ningún
     * módulo de negocio puede depender de otro módulo de negocio.
     */
    private static final String SHARED_MODULE = "shared";

    @Test
    void hasNoUnapprovedDependenciesBetweenModules() {
        ApplicationModules modules = ApplicationModules.of(SgoLogicApplication.class);

        modules.forEach(module -> {
            if (module.getIdentifier().toString().equals(SHARED_MODULE)) {
                assertThat(module.getDirectDependencies(modules).isEmpty())
                        .as("El módulo shared no debe depender de ningún módulo de negocio")
                        .isTrue();
                return;
            }
            Set<String> dependencyIds = module.getDirectDependencies(modules).stream()
                    .map(dependency -> dependency.getTargetModule().getIdentifier().toString())
                    .collect(Collectors.toSet());
            assertThat(dependencyIds)
                    .as("El módulo %s solo puede depender de shared hasta que el equipo apruebe otro contrato",
                            module.getIdentifier())
                    .isSubsetOf(Set.of(SHARED_MODULE));
        });
    }
}
