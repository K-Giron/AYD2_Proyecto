package com.megastore.sgologic.notifications;

import java.util.UUID;

/**
 * Puerto hacia el proveedor externo de SMS, correo y push (CDU-11).
 * {@link ChannelRoutingNotificationProvider} lo implementa: el correo sale
 * por SMTP real cuando hay credenciales configuradas y SMS/push siguen en
 * el simulador BB-06 ({@link SimulatedNotificationProviderAdapter}).
 * {@code referenceId} es la orden (o incidencia) de la que trata el aviso.
 */
public interface NotificationProviderPort {

    void send(NotificationChannel channel, String recipient, NotificationEventType eventType, UUID referenceId)
            throws NotificationProviderUnavailableException;
}
