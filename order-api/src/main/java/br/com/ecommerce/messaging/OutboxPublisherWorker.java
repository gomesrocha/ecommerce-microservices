package br.com.ecommerce.messaging;

import br.com.ecommerce.domain.OutboxEvent;
import br.com.ecommerce.service.OutboxService;
import io.quarkus.scheduler.Scheduled;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class OutboxPublisherWorker {

    private static final Logger LOG = Logger.getLogger(OutboxPublisherWorker.class);

    @Inject
    OutboxService outboxService;

    @Inject
    @Channel("stock-reservation-requested-out")
    Emitter<JsonObject> stockReservationRequestedEmitter;

    @Inject
    @Channel("order-created-out")
    Emitter<JsonObject> orderCreatedEmitter;

    @Inject
    @Channel("order-canceled-out")
    Emitter<JsonObject> orderCanceledEmitter;

    @Inject
    @Channel("notifications-requested-out")
    Emitter<JsonObject> notificationRequestedEmitter;

    @Inject
    @Channel("payment-requested-out")
    Emitter<JsonObject> paymentRequestedEmitter;

    @Scheduled(every = "{app.outbox.poll-interval}", delay = 10, delayUnit = TimeUnit.SECONDS)
    void publishPendingEvents() {
        List<OutboxEvent> events = outboxService.listPending(20);

        if (events.isEmpty()) {
            return;
        }

        for (OutboxEvent event : events) {
            publish(event);
        }
    }

    private void publish(OutboxEvent event) {
        try {
            JsonObject payload = new JsonObject(event.payload);

            Emitter<JsonObject> emitter = resolveEmitter(event.routingKey);

            emitter.send(payload)
                    .toCompletableFuture()
                    .get(10, TimeUnit.SECONDS);

            outboxService.markPublished(event.id);

            LOG.infof(
                    "Evento da outbox publicado. id=%s, eventType=%s, routingKey=%s, correlationId=%s",
                    event.id,
                    event.eventType,
                    event.routingKey,
                    event.correlationId
            );
        } catch (Exception exception) {
            LOG.errorf(
                    exception,
                    "Falha ao publicar evento da outbox. id=%s, eventType=%s, routingKey=%s, correlationId=%s",
                    event.id,
                    event.eventType,
                    event.routingKey,
                    event.correlationId
            );

            outboxService.markFailed(event.id, exception);
        }
    }

    private Emitter<JsonObject> resolveEmitter(String routingKey) {
        return switch (routingKey) {
            case "product.stock.reserve" -> stockReservationRequestedEmitter;
            case "order.created" -> orderCreatedEmitter;
            case "order.canceled" -> orderCanceledEmitter;
            case "notifications.requested" -> notificationRequestedEmitter;
            case "payment.requested" -> paymentRequestedEmitter;
            default -> throw new IllegalArgumentException("Routing key não suportada pela outbox: " + routingKey);
        };
    }
}