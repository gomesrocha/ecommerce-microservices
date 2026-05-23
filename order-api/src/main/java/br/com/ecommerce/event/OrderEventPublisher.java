package br.com.ecommerce.event;

import br.com.ecommerce.domain.Order;
import io.smallrye.reactive.messaging.rabbitmq.OutgoingRabbitMQMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;
import io.smallrye.reactive.messaging.rabbitmq.OutgoingRabbitMQMetadata;
import org.eclipse.microprofile.reactive.messaging.Message;
import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;


@ApplicationScoped
public class OrderEventPublisher {

    private static final Logger LOG = Logger.getLogger(OrderEventPublisher.class);

    @Inject
    @Channel("order-created-out")
    Emitter<OrderCreatedEvent> orderCreatedEmitter;

    @Inject
    @Channel("order-canceled-out")
    Emitter<OrderCanceledEvent> orderCanceledEmitter;

    @Inject
    @Channel("stock-reservation-requested-out")
    Emitter<StockReservationRequestedEvent> stockReservationRequestedEmitter;

    public void publishOrderCreated(Order order) {
        OrderCreatedEvent event = OrderCreatedEvent.fromEntity(order);

        orderCreatedEmitter
                .send(event)
                .whenComplete((unused, throwable) -> {
                    if (throwable != null) {
                        LOG.errorf(
                                throwable,
                                "Falha ao publicar evento OrderCreated para pedido %s",
                                order.id
                        );
                    } else {
                        LOG.infof(
                                "Evento OrderCreated publicado com sucesso para pedido %s",
                                order.id
                        );
                    }
                });
    }

    public void publishOrderCanceled(Order order) {
        OrderCanceledEvent event = OrderCanceledEvent.fromEntity(order);

        orderCanceledEmitter
                .send(event)
                .whenComplete((unused, throwable) -> {
                    if (throwable != null) {
                        LOG.errorf(
                                throwable,
                                "Falha ao publicar evento OrderCanceled para pedido %s",
                                order.id
                        );
                    } else {
                        LOG.infof(
                                "Evento OrderCanceled publicado com sucesso para pedido %s",
                                order.id
                        );
                    }
                });
    }
    public void publishStockReservationRequested(Order order) {
        StockReservationRequestedEvent event = StockReservationRequestedEvent.fromEntity(order);

        OutgoingRabbitMQMetadata metadata = new OutgoingRabbitMQMetadata.Builder()
                .withRoutingKey("product.stock.reserve")
                .withTimestamp(ZonedDateTime.now())
                .withHeader("eventType", "StockReservationRequested")
                .withHeader("sourceService", "order-api")
                .build();

        Message<StockReservationRequestedEvent> message = Message.of(
                event,
                () -> {
                    LOG.infof("Evento StockReservationRequested publicado para pedido %s", order.id);
                    return CompletableFuture.completedFuture(null);
                },
                throwable -> {
                    LOG.errorf(throwable, "Falha ao publicar StockReservationRequested para pedido %s", order.id);
                    return CompletableFuture.completedFuture(null);
                }
        ).addMetadata(metadata);

        stockReservationRequestedEmitter.send(message);
    }
}