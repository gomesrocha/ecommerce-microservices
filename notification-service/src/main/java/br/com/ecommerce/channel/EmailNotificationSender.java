package br.com.ecommerce.channel;

import br.com.ecommerce.domain.Notification;
import br.com.ecommerce.domain.NotificationChannel;
import br.com.ecommerce.domain.NotificationDelivery;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class EmailNotificationSender implements NotificationChannelSender {

    private static final Logger LOG = Logger.getLogger(EmailNotificationSender.class);

    @Inject
    Mailer mailer;

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public NotificationDeliveryResult send(Notification notification, NotificationDelivery delivery) {
        try {
            if (delivery.destination == null || delivery.destination.isBlank()) {
                return NotificationDeliveryResult.failed("Destino de e-mail não informado");
            }

            mailer.send(Mail.withText(
                    delivery.destination,
                    notification.title,
                    notification.message
            ));

            LOG.infof(
                    "E-mail enviado. notificationId=%s, destination=%s",
                    notification.id,
                    delivery.destination
            );

            return NotificationDeliveryResult.sent();

        } catch (Exception exception) {
            LOG.errorf(
                    exception,
                    "Falha ao enviar e-mail. notificationId=%s, destination=%s",
                    notification.id,
                    delivery.destination
            );

            return NotificationDeliveryResult.failed(exception.getMessage());
        }
    }
}