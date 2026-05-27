package br.com.ecommerce.repository;

import br.com.ecommerce.domain.Notification;
import br.com.ecommerce.domain.NotificationStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class NotificationRepository implements PanacheRepository<Notification> {

    public List<Notification> findByUserId(Long userId) {
        return list("userId = ?1 order by createdAt desc", userId);
    }

    public List<Notification> findUnreadByUserId(Long userId) {
        return list("userId = ?1 and status = ?2 order by createdAt desc", userId, NotificationStatus.CREATED);
    }
}