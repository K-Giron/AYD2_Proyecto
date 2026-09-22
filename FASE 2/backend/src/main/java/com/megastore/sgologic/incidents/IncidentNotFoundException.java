package com.megastore.sgologic.incidents;

import java.util.UUID;

public class IncidentNotFoundException extends RuntimeException {

    public IncidentNotFoundException(UUID id) {
        super("No existe una incidencia con id " + id);
    }
}
