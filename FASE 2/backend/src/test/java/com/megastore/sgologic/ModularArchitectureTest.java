package com.megastore.sgologic;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularArchitectureTest {

    @Test
    void verifiesDeclaredModuleBoundaries() {
        ApplicationModules.of(SgoLogicApplication.class).verify();
    }
}
