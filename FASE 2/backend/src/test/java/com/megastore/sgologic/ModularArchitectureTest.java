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

    @Test
    void hasNoUnapprovedDependenciesBetweenModules() {
        ApplicationModules modules = ApplicationModules.of(SgoLogicApplication.class);

        modules.forEach(module -> assertThat(module.getDirectDependencies(modules).isEmpty())
                .as("El módulo %s no debe declarar dependencias antes de aprobar el contrato",
                        module.getIdentifier())
                .isTrue());
    }
}
