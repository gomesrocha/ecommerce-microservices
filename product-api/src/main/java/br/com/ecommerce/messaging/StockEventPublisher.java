package br.com.ecommerce.messaging;

import br.com.ecommerce.event.StockRejectedEvent;
import br.com.ecommerce.event.StockReservedEvent;
import io.smallrye.reactive.messaging.rabbitmq.OutgoingRabbitMQMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;

@ApplicationScoped
public class StockEventPublisher {

    private static final Logger LOG = Logger.getLogger(StockEventPublisher.class);

    @Inject
    @Channel("stock-reserved-out")
    Emitter<StockReservedEvent> stockReservedEmitter;

    @Inject
    @Channel("stock-rejected-out")
    Emitter<StockRejectedEvent> stockRejectedEmitter;

    public void publishReserved(Long orderId, Long userId, String reason) {
        StockReservedEvent event = StockReservedEvent.of(orderId, userId, reason);

        OutgoingRabbitMQMetadata metadata = new OutgoingRabbitMQMetadata.Builder()
                .withRoutingKey("stock.reserved")
                .withTimestamp(ZonedDateTime.now())
                .withHeader("eventType", "StockReserved")
                .withHeader("sourceService", "product-api")
                .build();

        Message<StockReservedEvent> message = Message.of(
                event,
                () -> {
                    LOG.infof("Evento StockReserved publicado para pedido %s", orderId);
                    return CompletableFuture.completedFuture(null);
                },
                throwable -> {
                    LOG.errorf(throwable, "Falha ao publicar StockReserved para pedido %s", orderId);
                    return CompletableFuture.completedFuture(null);
                }
        ).addMetadata(metadata);

        stockReservedEmitter.send(message);
    }

    public void publishRejected(Long orderId, Long userId, String reason) {
        StockRejectedEvent event = StockRejectedEvent.of(orderId, userId, reason);

        OutgoingRabbitMQMetadata metadata = new OutgoingRabbitMQMetadata.Builder()
                .withRoutingKey("stock.rejected")
                .withTimestamp(ZonedDateTime.now())
                .withHeader("eventType", "StockRejected")
                .withHeader("sourceService", "product-api")
                .build();

        Message<StockRejectedEvent> message = Message.of(
                event,
                () -> {
                    LOG.infof("Evento StockRejected publicado para pedido %s", orderId);
                    return CompletableFuture.completedFuture(null);
                },
                throwable -> {
                    LOG.errorf(throwable, "Falha ao publicar StockRejected para pedido %s", orderId);
                    return CompletableFuture.completedFuture(null);
                }
        ).addMetadata(metadata);

        stockRejectedEmitter.send(message);
    }
}