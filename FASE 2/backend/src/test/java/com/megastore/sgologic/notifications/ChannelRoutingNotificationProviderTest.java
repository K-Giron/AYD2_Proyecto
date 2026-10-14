package com.megastore.sgologic.notifications;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ChannelRoutingNotificationProviderTest {

    private final NotificationProviderPort simulated = mock(NotificationProviderPort.class);
    private final SmtpEmailSender smtp = mock(SmtpEmailSender.class);
    private final UUID orderId = UUID.randomUUID();

    private ChannelRoutingNotificationProvider provider(String mode, String user, String password) {
        EmailNotificationProperties properties = new EmailNotificationProperties();
        properties.setProvider(mode);
        return new ChannelRoutingNotificationProvider(simulated, smtp, properties, user, password);
    }

    @Test
    void emailGoesThroughSmtpWhenCredentialsAreConfigured() throws Exception {
        ChannelRoutingNotificationProvider provider = provider("auto", "megastore@gmail.com", "app-password");

        provider.send(NotificationChannel.EMAIL, "ana@correo.com", NotificationEventType.ORDER_CONFIRMED, orderId);

        assertThat(provider.sendsRealEmail()).isTrue();
        verify(smtp).send("ana@correo.com", NotificationEventType.ORDER_CONFIRMED, orderId);
        verify(simulated, never()).send(any(), any(), any(), any());
    }

    @Test
    void withoutCredentialsEmailStaysInTheSimulator() throws Exception {
        ChannelRoutingNotificationProvider provider = provider("auto", "", "");

        provider.send(NotificationChannel.EMAIL, "ana@correo.com", NotificationEventType.ORDER_CONFIRMED, orderId);

        assertThat(provider.sendsRealEmail()).isFalse();
        verify(simulated).send(NotificationChannel.EMAIL, "ana@correo.com", NotificationEventType.ORDER_CONFIRMED, orderId);
        verify(smtp, never()).send(any(), any(), any());
    }

    @Test
    void smsAndPushAreAlwaysSimulated() throws Exception {
        ChannelRoutingNotificationProvider provider = provider("smtp", "megastore@gmail.com", "app-password");

        provider.send(NotificationChannel.SMS, "+50255550000", NotificationEventType.SHIPMENT_IN_TRANSIT, orderId);

        verify(simulated).send(NotificationChannel.SMS, "+50255550000", NotificationEventType.SHIPMENT_IN_TRANSIT, orderId);
        verify(smtp, never()).send(any(), any(), any());
    }
}
