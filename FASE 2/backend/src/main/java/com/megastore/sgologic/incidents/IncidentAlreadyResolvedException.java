package com.megastore.sgologic.incidents;

import java.util.UUID;

public class IncidentAlreadyResolvedException extends RuntimeException {

    public IncidentAlreadyResolvedException(UUID id, IncidentStatus currentStatus) {
        super("La incidencia " + id + " ya fue resuelta con estado " + currentStatus);
    }
}
