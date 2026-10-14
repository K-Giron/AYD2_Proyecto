package com.megastore.sgologic.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

/**
 * Elige el proveedor por canal: el correo sale por SMTP real cuando está
 * configurado (usuario y contraseña en el entorno); SMS y push, y el correo
 * sin credenciales, siguen en el simulador BB-06.
 */
@Primary
@Component
class ChannelRoutingNotificationProvider implements NotificationProviderPort {

    private static final Logger log = LoggerFactory.getLogger(ChannelRoutingNotificationProvider.class);

    private final NotificationProviderPort simulated;
    private final SmtpEmailSender smtp;
    private final boolean emailBySmtp;

    ChannelRoutingNotificationProvider(@Qualifier("simulatedNotificationProvider") NotificationProviderPort simulated,
                                       SmtpEmailSender smtp, EmailNotificationProperties properties,
                                       @Value("${spring.mail.username:}") String username,
                                       @Value("${spring.mail.password:}") String password) {
        this.simulated = simulated;
        this.smtp = smtp;
        String mode = properties.getProvider() == null ? "auto" : properties.getProvider().trim().toLowerCase(Locale.ROOT);
        boolean hasCredentials = !username.isBlank() && !password.isBlank();
        this.emailBySmtp = mode.equals("smtp") || (mode.equals("auto") && hasCredentials);
        log.info("Notificaciones por correo: {}", emailBySmtp
                ? "SMTP real como " + username : "simulador (sin credenciales SMTP configuradas)");
    }

    @Override
    public void send(NotificationChannel channel, String recipient, NotificationEventType eventType, UUID referenceId)
            throws NotificationProviderUnavailableException {
        if (channel == NotificationChannel.EMAIL && emailBySmtp) {
            smtp.send(recipient, eventType, referenceId);
            return;
        }
        simulated.send(channel, recipient, eventType, referenceId);
    }

    boolean sendsRealEmail() {
        return emailBySmtp;
    }
}
