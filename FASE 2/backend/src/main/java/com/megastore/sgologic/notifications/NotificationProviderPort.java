package com.megastore.sgologic.notifications;

/**
 * Puerto hacia el proveedor externo de SMS, correo y push (CDU-11).
 * {@link SimulatedNotificationProviderAdapter} lo implementa de forma
 * temporal hasta que BB-06 entregue el simulador oficial de Andy Sánchez.
 */
public interface NotificationProviderPort {

    void send(NotificationChannel channel, String recipient, NotificationEventType eventType)
            throws NotificationProviderUnavailableException;
}
