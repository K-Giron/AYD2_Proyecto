package com.megastore.sgologic.audit;

/**
 * CDU-12 E1: no existe evidencia para el identificador consultado. La
 * búsqueda fallida queda registrada y se alerta a Finanzas para
 * investigación, sin inventar información.
 */
class AuditEvidenceNotFoundException extends RuntimeException {

    AuditEvidenceNotFoundException(String criterion) {
        super("No existe evidencia auditable para " + criterion + ".");
    }
}
