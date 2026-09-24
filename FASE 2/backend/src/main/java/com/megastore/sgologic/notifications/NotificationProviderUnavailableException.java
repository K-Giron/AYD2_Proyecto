package com.megastore.sgologic.notifications;

/** El proveedor externo de notificaciones no está disponible (CDU-11, E1). */
public class NotificationProviderUnavailableException extends Exception {

    public NotificationProviderUnavailableException(String message) {
        super(message);
    }
}
