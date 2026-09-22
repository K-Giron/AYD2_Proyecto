package com.megastore.sgologic.logistics;

/**
 * Estados de despacho descritos en CDU-07 (preparación, tránsito, entrega) y
 * en CDU-08 para la guía de LogiExpress. No reemplaza ni amplía el
 * vocabulario propuesto en {@code order-states.md}: es un estado interno de
 * este módulo, correlacionado por {@code orderId}.
 */
public enum ShipmentStatus {
    ASSIGNED,
    PREPARING,
    LABEL_REQUESTED,
    LABEL_ISSUED,
    LABEL_FAILED,
    READY_FOR_PICKUP,
    IN_TRANSIT,
    DELIVERED,
    RETURNED
}
