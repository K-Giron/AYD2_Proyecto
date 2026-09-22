package com.megastore.sgologic.notifications;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Adaptador temporal del proveedor de SMS/correo/push mientras BB-06
 * (simuladores configurables de Andy Sánchez) no está disponible. Simula una
 * tasa de indisponibilidad para ejercitar el flujo no bloqueante de NOT-01.
 */
@Component
class SimulatedNotificationProviderAdapter implements NotificationProviderPort {

    private static final double UNAVAILABLE_RATE = 0.05;

    @Override
    public void send(NotificationChannel channel, String recipient, NotificationEventType eventType)
            throws NotificationProviderUnavailableException {
        if (ThreadLocalRandom.current().nextDouble() < UNAVAILABLE_RATE) {
            throw new NotificationProviderUnavailableException(
                    "El proveedor de " + channel + " no está disponible para el evento " + eventType);
        }
    }
}
