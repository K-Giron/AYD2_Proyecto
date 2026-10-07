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
            "messaging",
            "integration",
            "inventorysync",
            "customers",
            "onlinestore",
            "legacyfeed",
            "salesreport",
            "fulfillment",
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
     * {@code integration.outbox.IdempotentConsumer}, BB-04) y {@code audit}
     * su API pública de evidencia ({@code audit.api.AuditRecorder}, AUD-01).
     * El resto de cada módulo es interno:
     * {@code ApplicationModules.verify()} rechaza el acceso a subpaquetes no
     * anotados con {@code @NamedInterface}.</p>
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
            Set<String> allowed = switch (module.getIdentifier().toString()) {
                case "orders" -> Set.of(SHARED_MODULE, "inventory", "payments", "fiscal",
                        "integration", "audit", "logistics");
                case "query" -> Set.of(SHARED_MODULE, "messaging");
                case "notifications" -> Set.of(SHARED_MODULE, "messaging", "customers");
                case "payments", "logistics" -> Set.of(SHARED_MODULE, "integration", "audit");
                case "incidents" -> Set.of(SHARED_MODULE, "audit", "integration");
                case "inventory" -> Set.of(SHARED_MODULE, "integration");
                case "inventorysync" -> Set.of(SHARED_MODULE, "orders", "messaging", "onlinestore");
                case "onlinestore" -> Set.of(SHARED_MODULE, "inventory");
                case "legacyfeed" -> Set.of(SHARED_MODULE, "messaging", "orders", "logistics", "integration");
                case "fulfillment" -> Set.of(SHARED_MODULE, "messaging", "orders");
                case "salesreport" -> Set.of(SHARED_MODULE, "orders", "payments", "fiscal", "integration", "audit");
                default -> Set.of(SHARED_MODULE);
            };
            assertThat(dependencyIds)
                    .as("El módulo %s solo puede depender de contratos aprobados", module.getIdentifier())
                    .isSubsetOf(allowed);
        });
    }
}
