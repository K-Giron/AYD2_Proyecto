package com.megastore.sgologic.notifications;

/**
 * Hechos de negocio notificables descritos en CUN-11/CDU-11: confirmación,
 * preparación, tránsito, entrega o incidencia. Las confirmaciones de orden
 * se reciben automáticamente mediante Outbox/Inbox; la API se conserva para
 * simulación y eventos cuyos productores aún no han sido conectados.
 */
public enum NotificationEventType {
    ORDER_CONFIRMED,
    SHIPMENT_PREPARING,
    SHIPMENT_IN_TRANSIT,
    SHIPMENT_DELIVERED,
    INCIDENT_REGISTERED
}
