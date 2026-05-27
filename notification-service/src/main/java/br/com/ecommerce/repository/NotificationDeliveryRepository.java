package br.com.ecommerce.repository;

import br.com.ecommerce.domain.NotificationDelivery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class NotificationDeliveryRepository implements PanacheRepository<NotificationDelivery> {

    public List<NotificationDelivery> findByNotificationId(Long notificationId) {
        return list("notification.id = ?1 order by createdAt asc", notificationId);
    }
}