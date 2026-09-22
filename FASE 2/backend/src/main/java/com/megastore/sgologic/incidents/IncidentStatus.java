package com.megastore.sgologic.incidents;

/**
 * CDU-10: registrada, en evaluación, o resuelta (aprobada/rechazada/con
 * reembolso procesado). El sistema no continúa con el reembolso hasta que
 * el caso se resuelve manualmente (E1) mientras AUD-01 y FIS-02 no están
 * disponibles para validar elegibilidad automáticamente.
 */
public enum IncidentStatus {
    REGISTERED,
    EVALUATING,
    APPROVED,
    REJECTED,
    REFUNDED
}
