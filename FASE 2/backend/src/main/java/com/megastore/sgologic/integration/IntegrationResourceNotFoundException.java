package com.megastore.sgologic.integration;

import java.util.UUID;

class IntegrationResourceNotFoundException extends RuntimeException {
    IntegrationResourceNotFoundException(String resource, UUID id) {
        super("No existe " + resource + " con identificador " + id + ".");
    }
}
