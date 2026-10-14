package com.megastore.sgologic.notifications;

import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Envío SMTP real contra un servidor de correo de prueba (Mailpit): el aviso
 * llega con su asunto, destinatario y enlace; si el servidor no responde, el
 * error se traduce a proveedor no disponible (reintento y bandeja de fallidos).
 */
@Testcontainers
class SmtpEmailSenderMailpitTest {

    @Container
    static final GenericContainer<?> MAILPIT = new GenericContainer<>(DockerImageName.parse("axllent/mailpit:v1.27"))
            .withExposedPorts(1025, 8025)
            .waitingFor(Wait.forHttp("/api/v1/messages").forPort(8025));

    private SmtpEmailSender sender(String host, int port) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.getJavaMailProperties().put("mail.smtp.connectiontimeout", "3000");
        mailSender.getJavaMailProperties().put("mail.smtp.timeout", "3000");
        EmailNotificationProperties properties = new EmailNotificationProperties();
        properties.setFrom("notificaciones@megastore.test");
        properties.setSiteUrl("http://localhost:8081");
        return new SmtpEmailSender(mailSender, properties, "");
    }

    @Test
    void deliversARealEmailWithSubjectRecipientAndLink() throws Exception {
        UUID orderId = UUID.fromString("7a126040-3edc-4374-af71-041b0814940c");

        sender(MAILPIT.getHost(), MAILPIT.getMappedPort(1025))
                .send("ana@correo.com", NotificationEventType.SHIPMENT_IN_TRANSIT, orderId);

        String inbox = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(
                        "http://" + MAILPIT.getHost() + ":" + MAILPIT.getMappedPort(8025) + "/api/v1/messages")).build(),
                HttpResponse.BodyHandlers.ofString()).body();
        assertThat(inbox).contains("ana@correo.com");
        assertThat(inbox).contains("Tu pedido 7A126040 va en camino");
        assertThat(inbox).contains("notificaciones@megastore.test");
    }

    @Test
    void unreachableMailServerIsReportedAsProviderUnavailable() {
        assertThatThrownBy(() -> sender("127.0.0.1", 9).send("ana@correo.com",
                NotificationEventType.ORDER_CONFIRMED, UUID.randomUUID()))
                .isInstanceOf(NotificationProviderUnavailableException.class);
    }
}
