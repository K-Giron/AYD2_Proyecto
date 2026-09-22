package com.megastore.sgologic.notifications;

import java.util.UUID;

public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException(UUID id) {
        super("No existe una solicitud de notificación con id " + id);
    }
}
