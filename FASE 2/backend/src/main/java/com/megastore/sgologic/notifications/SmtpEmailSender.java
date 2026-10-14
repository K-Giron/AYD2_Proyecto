package com.megastore.sgologic.notifications;

import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Envía el aviso como un correo real por SMTP (Gmail u otro servidor). Una
 * falla del servidor de correo se traduce a
 * {@link NotificationProviderUnavailableException}, así entra al mismo
 * reintento y bandeja de fallidos que cualquier proveedor externo y nunca
 * afecta la orden.
 */
@Component
class SmtpEmailSender {

    private final JavaMailSender mailSender;
    private final EmailNotificationProperties properties;
    private final String smtpUsername;

    SmtpEmailSender(JavaMailSender mailSender, EmailNotificationProperties properties,
                    @org.springframework.beans.factory.annotation.Value("${spring.mail.username:}") String smtpUsername) {
        this.mailSender = mailSender;
        this.properties = properties;
        this.smtpUsername = smtpUsername;
    }

    void send(String recipient, NotificationEventType eventType, UUID referenceId)
            throws NotificationProviderUnavailableException {
        EmailContent content = EmailContent.of(eventType, referenceId, properties.getSiteUrl());
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            String from = properties.getFrom() == null || properties.getFrom().isBlank() ? smtpUsername : properties.getFrom();
            helper.setFrom(from, properties.getFromName());
            helper.setTo(recipient);
            helper.setSubject(content.subject());
            helper.setText(content.text(), content.html());
            mailSender.send(message);
        } catch (MailException | jakarta.mail.MessagingException | java.io.UnsupportedEncodingException failure) {
            throw new NotificationProviderUnavailableException(
                    "El servidor de correo no aceptó el aviso " + eventType + ": " + failure.getMessage());
        }
    }

    /** Asunto y cuerpo (texto y HTML) de cada aviso de CDU-11. */
    record EmailContent(String subject, String text, String html) {

        static EmailContent of(NotificationEventType eventType, UUID referenceId, String siteUrl) {
            String shortId = referenceId == null ? "" : referenceId.toString().substring(0, 8).toUpperCase();
            String[] copy = switch (eventType) {
                case ORDER_CONFIRMED -> new String[]{"Tu compra " + shortId + " fue confirmada",
                        "¡Gracias por tu compra!", "La operación financiera y tu factura electrónica fueron confirmadas. Ya estamos preparando tu pedido."};
                case CREDIT_GRANTED -> new String[]{"Tu crédito para la compra " + shortId + " fue aprobado",
                        "Crédito propio aprobado", "Aprobamos tu compra para pagar en 30 días. Consulta tu pedido para ver el detalle y la fecha de vencimiento."};
                case CREDIT_REJECTED -> new String[]{"Resultado del crédito para la compra " + shortId,
                        "Crédito no disponible para esta compra", "Esta vez no pudimos aprobar el crédito propio. Tu orden puede continuar de forma segura con PayKlik."};
                case SHIPMENT_PREPARING -> new String[]{"Estamos preparando tu pedido " + shortId,
                        "Tu pedido está en preparación", "Estamos alistando tus productos. Te avisaremos cuando salga hacia su destino."};
                case SHIPMENT_IN_TRANSIT -> new String[]{"Tu pedido " + shortId + " va en camino",
                        "Tu pedido va en camino", "Tu pedido ya salió con LogiExpress hacia el destino que elegiste."};
                case SHIPMENT_READY_FOR_PICKUP -> new String[]{"Tu pedido " + shortId + " está listo para recoger",
                        "Tu pedido ya está en la tienda", "LogiExpress dejó tu pedido en la tienda que elegiste. Pasa a recogerlo con tu número de pedido y un documento de identificación."};
                case SHIPMENT_DELIVERED -> new String[]{"Tu pedido " + shortId + " fue entregado",
                        "¡Pedido entregado!", "Tu pedido fue entregado. Si algo no está bien, puedes solicitar una devolución desde tu cuenta."};
                case INCIDENT_REGISTERED -> new String[]{"Recibimos tu solicitud sobre el pedido " + shortId,
                        "Recibimos tu solicitud", "Registramos tu devolución o incidencia y nuestro equipo de atención la revisará."};
            };
            String link = siteUrl + "/cliente.html#tracking";
            String text = copy[1] + "\n\n" + copy[2] + "\n\nPedido: " + (referenceId == null ? "—" : referenceId)
                    + "\nConsulta su estado en: " + link + "\n\nMegaStore";
            String html = """
                    <div style="font-family:Arial,Helvetica,sans-serif;max-width:560px;margin:auto;color:#0a1930">
                      <div style="background:#071f45;color:#fff;padding:18px 24px;font-size:20px;font-weight:bold">MegaStore</div>
                      <div style="padding:24px;border:1px solid #e5e7eb;border-top:0">
                        <h2 style="margin:0 0 12px">%s</h2>
                        <p style="line-height:1.6;color:#374151">%s</p>
                        <p style="color:#6b7280;font-size:13px">Pedido <strong>%s</strong></p>
                        <p><a href="%s" style="display:inline-block;background:#08a9d6;color:#04152e;padding:10px 18px;text-decoration:none;font-weight:bold">Ver mi pedido</a></p>
                      </div>
                      <p style="color:#9ca3af;font-size:12px;text-align:center">Recibes este correo porque compraste en MegaStore.</p>
                    </div>""".formatted(copy[1], copy[2], referenceId == null ? "—" : referenceId, link);
            return new EmailContent(copy[0], text, html);
        }
    }
}
