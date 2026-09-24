/**
 * Es propietario de las solicitudes y resultados de notificación.
 */
@org.springframework.modulith.ApplicationModule(
        id = "notifications",
        displayName = "Notifications",
        allowedDependencies = {"shared", "messaging"}
)
package com.megastore.sgologic.notifications;
