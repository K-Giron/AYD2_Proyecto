package com.megastore.sgologic.notifications;

/**
 * Hechos de negocio notificables descritos en CUN-11/CDU-11: confirmación,
 * preparación, tránsito, entrega o incidencia. Se recibe por API mientras el
 * sobre de eventos v1 y el Outbox (BB-04) no están ratificados.
 */
public enum NotificationEventType {
    ORDER_CONFIRMED,
    SHIPMENT_PREPARING,
    SHIPMENT_IN_TRANSIT,
    SHIPMENT_DELIVERED,
    INCIDENT_REGISTERED
}
