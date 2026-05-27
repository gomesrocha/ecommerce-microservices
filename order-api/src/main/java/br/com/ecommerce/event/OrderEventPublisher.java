package br.com.ecommerce.event;

import br.com.ecommerce.domain.Order;
import br.com.ecommerce.service.OutboxService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import br.com.ecommerce.notification.NotificationRequestEvent;

@ApplicationScoped
public class OrderEventPublisher {

    private static final Logger LOG = Logger.getLogger(OrderEventPublisher.class);

    @Inject
    OutboxService outboxService;

    public void publishOrderCreated(Order order) {
        OrderCreatedEvent event = OrderCreatedEvent.fromEntity(order);

        outboxService.saveEvent(
                "Order",
                order.id,
                "OrderCreated",
                "order.created",
                event
        );

        LOG.infof("Evento OrderCreated salvo na outbox para pedido %s", order.id);
    }

    public void publishOrderCanceled(Order order) {
        OrderCanceledEvent event = OrderCanceledEvent.fromEntity(order);

        outboxService.saveEvent(
                "Order",
                order.id,
                "OrderCanceled",
                "order.canceled",
                event
        );

        LOG.infof("Evento OrderCanceled salvo na outbox para pedido %s", order.id);
    }

    public void publishStockReservationRequested(Order order) {
        StockReservationRequestedEvent event = StockReservationRequestedEvent.fromEntity(order);

        outboxService.saveEvent(
                "Order",
                order.id,
                "StockReservationRequested",
                "product.stock.reserve",
                event
        );

        LOG.infof("Evento StockReservationRequested salvo na outbox para pedido %s", order.id);
    }

    public void publishOrderConfirmedNotification(Order order) {
        NotificationRequestEvent event = NotificationRequestEvent.orderConfirmed(
                order.id,
                order.userId,
                order.totalAmount,
                order.customerState
        );

        outboxService.saveEvent(
                "Order",
                order.id,
                "NotificationRequested",
                "notifications.requested",
                event
        );

        LOG.infof("NotificationRequested ORDER_CONFIRMED salvo na outbox para pedido %s", order.id);
    }

    public void publishOrderCanceledNotification(Order order, String reason) {
        NotificationRequestEvent event = NotificationRequestEvent.orderCanceled(
                order.id,
                order.userId,
                reason
        );

        outboxService.saveEvent(
                "Order",
                order.id,
                "NotificationRequested",
                "notifications.requested",
                event
        );

        LOG.infof("NotificationRequested ORDER_CANCELED salvo na outbox para pedido %s", order.id);
    }

    public void publishOrderRejectedNotification(Order order, String reason) {
        NotificationRequestEvent event = NotificationRequestEvent.orderRejected(
                order.id,
                order.userId,
                reason
        );

        outboxService.saveEvent(
                "Order",
                order.id,
                "NotificationRequested",
                "notifications.requested",
                event
        );

        LOG.infof("NotificationRequested ORDER_REJECTED salvo na outbox para pedido %s", order.id);
    }
}