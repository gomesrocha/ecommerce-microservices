package br.com.ecommerce.service;

import br.com.ecommerce.domain.Order;
import br.com.ecommerce.domain.OrderStatus;
import br.com.ecommerce.domain.OrderStatusChangeTrigger;
import br.com.ecommerce.domain.OrderStatusHistory;
import br.com.ecommerce.repository.OrderStatusHistoryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class OrderStateMachineService {

    @Inject
    OrderStatusHistoryRepository historyRepository;

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
        OrderStatus.CREATED, Set.of(OrderStatus.WAITING_STOCK, OrderStatus.CANCELED),
        OrderStatus.WAITING_STOCK, Set.of(OrderStatus.WAITING_FRAUD, OrderStatus.REJECTED, OrderStatus.CANCELED),
        OrderStatus.WAITING_FRAUD, Set.of(OrderStatus.WAITING_PAYMENT, OrderStatus.REJECTED, OrderStatus.CANCELED),
        OrderStatus.WAITING_PAYMENT, Set.of(OrderStatus.CONFIRMED, OrderStatus.REJECTED, OrderStatus.CANCELED),
        OrderStatus.CONFIRMED, Set.of(OrderStatus.CANCELED),
        OrderStatus.REJECTED, Set.of(),
        OrderStatus.CANCELED, Set.of()
);

    public void transition(
            Order order,
            OrderStatus newStatus,
            OrderStatusChangeTrigger trigger,
            String reason
    ) {
        if (order == null || order.id == null) {
            throw new BadRequestException("Pedido inválido para transição de status");
        }

        OrderStatus previousStatus = order.status;

        if (previousStatus != null && previousStatus.equals(newStatus)) {
            registerHistory(order, previousStatus, newStatus, trigger, reason);
            return;
        }

        validateTransition(previousStatus, newStatus);

        order.status = newStatus;

        registerHistory(order, previousStatus, newStatus, trigger, reason);
    }

    private void validateTransition(OrderStatus previousStatus, OrderStatus newStatus) {
        if (previousStatus == null) {
            return;
        }

        Set<OrderStatus> allowedStatuses = ALLOWED_TRANSITIONS.getOrDefault(previousStatus, Set.of());

        if (!allowedStatuses.contains(newStatus)) {
            throw new BadRequestException(
                    "Transição de status inválida: " + previousStatus + " -> " + newStatus
            );
        }
    }

    private void registerHistory(
            Order order,
            OrderStatus previousStatus,
            OrderStatus newStatus,
            OrderStatusChangeTrigger trigger,
            String reason
    ) {
        OrderStatusHistory history = new OrderStatusHistory();

        history.orderId = order.id;
        history.previousStatus = previousStatus;
        history.newStatus = newStatus;
        history.triggerEvent = trigger;
        history.reason = reason;

        historyRepository.persist(history);
    }
}