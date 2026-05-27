package br.com.ecommerce.service;

import br.com.ecommerce.channel.NotificationDispatcher;
import br.com.ecommerce.domain.*;
import br.com.ecommerce.dto.NotificationRequest;
import br.com.ecommerce.dto.NotificationResponse;
import br.com.ecommerce.metrics.NotificationMetricsService;
import br.com.ecommerce.repository.NotificationDeliveryRepository;
import br.com.ecommerce.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class NotificationService {

    @Inject
    NotificationRepository notificationRepository;

    @Inject
    NotificationDeliveryRepository deliveryRepository;

    @Inject
    NotificationDispatcher dispatcher;

    @Inject
    NotificationMetricsService metricsService;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "app.notification.default-email", defaultValue = "cliente.demo@ecommerce.local")
    String defaultEmail;

    @Transactional
    public NotificationResponse create(NotificationRequest request) {
        Notification notification = new Notification();
        notification.eventId = request.eventId();
        notification.eventType = request.eventType();
        notification.aggregateType = request.aggregateType();
        notification.aggregateId = request.aggregateId();
        notification.userId = request.userId();
        notification.title = request.title();
        notification.message = request.message();
        notification.severity = request.severity() == null ? NotificationSeverity.INFO : request.severity();
        notification.status = NotificationStatus.CREATED;
        notification.metadataJson = toJson(request.metadata());
        notification.createdAt = LocalDateTime.now();

        notificationRepository.persist(notification);

        List<NotificationChannel> channels = request.channels();

        for (NotificationChannel channel : channels) {
            NotificationDelivery delivery = new NotificationDelivery();
            delivery.notification = notification;
            delivery.channel = channel;
            delivery.destination = resolveDestination(channel, request);
            delivery.status = NotificationDeliveryStatus.PENDING;
            delivery.attempts = 0;
            delivery.createdAt = LocalDateTime.now();
            delivery.updatedAt = LocalDateTime.now();

            deliveryRepository.persist(delivery);

            dispatcher.dispatch(notification, delivery);

            metricsService.recordDelivery(
                    delivery.channel,
                    delivery.status,
                    notification.eventType
            );
        }

        return NotificationResponse.fromEntity(notification);
    }

    public List<NotificationResponse> listByUser(Long userId) {
        return notificationRepository.findByUserId(userId)
                .stream()
                .map(NotificationResponse::fromEntity)
                .toList();
    }

    public List<NotificationResponse> listUnreadByUser(Long userId) {
        return notificationRepository.findUnreadByUserId(userId)
                .stream()
                .map(NotificationResponse::fromEntity)
                .toList();
    }

    @Transactional
    public NotificationResponse markAsRead(Long id) {
        Notification notification = notificationRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Notificação não encontrada: " + id));

        notification.status = NotificationStatus.READ;
        notification.readAt = LocalDateTime.now();

        return NotificationResponse.fromEntity(notification);
    }

    public NotificationResponse findById(Long id) {
        Notification notification = notificationRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Notificação não encontrada: " + id));

        return NotificationResponse.fromEntity(notification);
    }

    private String resolveDestination(NotificationChannel channel, NotificationRequest request) {
        return switch (channel) {
            case SCREEN -> request.userId() == null ? "anonymous" : "user:" + request.userId();
            case EMAIL -> request.email() == null || request.email().isBlank() ? defaultEmail : request.email();
            case WHATSAPP -> "not-implemented";
        };
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{}";
        }
    }
}