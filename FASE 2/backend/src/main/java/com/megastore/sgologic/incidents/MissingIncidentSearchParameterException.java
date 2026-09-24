package com.megastore.sgologic.incidents;

public class MissingIncidentSearchParameterException extends RuntimeException {

    public MissingIncidentSearchParameterException() {
        super("Se requiere orderId o customerId para buscar incidencias.");
    }
}
