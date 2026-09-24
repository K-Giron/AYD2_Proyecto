package com.megastore.sgologic.notifications;

/**
 * Puerto hacia el proveedor externo de SMS, correo y push (CDU-11).
 * {@link SimulatedNotificationProviderAdapter} es su implementación BB-06
 * para desarrollo y pruebas.
 */
public interface NotificationProviderPort {

    void send(NotificationChannel channel, String recipient, NotificationEventType eventType)
            throws NotificationProviderUnavailableException;
}
