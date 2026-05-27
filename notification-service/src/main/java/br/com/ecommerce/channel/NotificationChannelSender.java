package br.com.ecommerce.channel;

import br.com.ecommerce.domain.Notification;
import br.com.ecommerce.domain.NotificationChannel;
import br.com.ecommerce.domain.NotificationDelivery;

public interface NotificationChannelSender {

    NotificationChannel channel();

    NotificationDeliveryResult send(Notification notification, NotificationDelivery delivery);
}