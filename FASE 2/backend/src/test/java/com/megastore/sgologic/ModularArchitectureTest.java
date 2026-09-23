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
     * {@code FASE 2/docs/integration/module-delivery-checklist.md}.
     *
     * <p>{@code integration} expone únicamente su API pública de Outbox
     * ({@code integration.outbox.PublishEvent} e
     * {@code integration.outbox.IdempotentConsumer}, BB-04). El resto del
     * módulo es interno: {@code ApplicationModules.verify()} rechaza el
     * acceso a subpaquetes no anotados con {@code @NamedInterface}.</p>
     */
    private static final String SHARED_MODULE = "shared";
    private static final String INTEGRATION_MODULE = "integration";
    private static final Set<String> APPROVED_DEPENDENCIES = Set.of(SHARED_MODULE, INTEGRATION_MODULE);

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
                    .as("El módulo %s solo puede depender de shared o de la API pública de "
                            + "integration.outbox (BB-04); cualquier otro contrato requiere aprobación",
                            module.getIdentifier())
                    .isSubsetOf(APPROVED_DEPENDENCIES);
        });
    }
}
