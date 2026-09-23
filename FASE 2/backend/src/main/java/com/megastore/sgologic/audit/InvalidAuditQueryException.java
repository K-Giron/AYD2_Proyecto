package com.megastore.sgologic.audit;

/** La consulta de evidencia no indicó un criterio válido. */
class InvalidAuditQueryException extends RuntimeException {

    InvalidAuditQueryException(String message) {
        super(message);
    }
}
