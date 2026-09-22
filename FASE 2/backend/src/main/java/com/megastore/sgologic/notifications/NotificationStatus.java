package com.megastore.sgologic.notifications;

/** CDU-11, postcondición: "la notificación queda entregada o registrada como pendiente/fallida". */
public enum NotificationStatus {
    PENDING,
    SENT,
    FAILED
}
