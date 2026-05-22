package br.com.ecommerce.event;

import br.com.ecommerce.domain.Order;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

@ApplicationScoped
public class OrderEventPublisher {

    private static final Logger LOG = Logger.getLogger(OrderEventPublisher.class);

    @Inject
    @Channel("order-created-out")
    Emitter<OrderCreatedEvent> orderCreatedEmitter;

    @Inject
    @Channel("order-canceled-out")
    Emitter<OrderCanceledEvent> orderCanceledEmitter;

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
}