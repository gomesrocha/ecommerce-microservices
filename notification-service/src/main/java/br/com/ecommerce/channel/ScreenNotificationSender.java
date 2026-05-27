package br.com.ecommerce.channel;

import br.com.ecommerce.domain.Notification;
import br.com.ecommerce.domain.NotificationChannel;
import br.com.ecommerce.domain.NotificationDelivery;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ScreenNotificationSender implements NotificationChannelSender {

    private static final Logger LOG = Logger.getLogger(ScreenNotificationSender.class);

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SCREEN;
    }

    @Override
    public NotificationDeliveryResult send(Notification notification, NotificationDelivery delivery) {
        LOG.infof(
                "Notificação SCREEN registrada. notificationId=%s, userId=%s, title=%s",
                notification.id,
                notification.userId,
                notification.title
        );

        return NotificationDeliveryResult.sent();
    }
}