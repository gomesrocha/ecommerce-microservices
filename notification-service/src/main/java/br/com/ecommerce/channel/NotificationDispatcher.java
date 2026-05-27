package br.com.ecommerce.channel;

import br.com.ecommerce.domain.Notification;
import br.com.ecommerce.domain.NotificationDelivery;
import br.com.ecommerce.domain.NotificationDeliveryStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.time.LocalDateTime;

@ApplicationScoped
public class NotificationDispatcher {

    @Inject
    Instance<NotificationChannelSender> senders;

    public void dispatch(Notification notification, NotificationDelivery delivery) {
        delivery.attempts = delivery.attempts == null ? 1 : delivery.attempts + 1;
        delivery.updatedAt = LocalDateTime.now();

        NotificationDeliveryResult result = senders.stream()
                .filter(sender -> sender.channel().equals(delivery.channel))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Canal não suportado: " + delivery.channel))
                .send(notification, delivery);

        if (result.success()) {
            delivery.status = NotificationDeliveryStatus.SENT;
            delivery.sentAt = LocalDateTime.now();
            delivery.lastError = null;
        } else {
            delivery.status = NotificationDeliveryStatus.FAILED;
            delivery.lastError = result.errorMessage();
        }

        delivery.updatedAt = LocalDateTime.now();
    }
}