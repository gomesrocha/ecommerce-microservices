package br.com.ecommerce.service;

import br.com.ecommerce.client.DeliveryEstimateClient;
import br.com.ecommerce.client.DeliveryEstimateClientRequest;
import br.com.ecommerce.client.DeliveryEstimateClientResponse;
import br.com.ecommerce.client.ProductClient;
import br.com.ecommerce.client.ProductClientResponse;
import br.com.ecommerce.domain.Order;
import br.com.ecommerce.domain.OrderItem;
import br.com.ecommerce.domain.OrderStatus;
import br.com.ecommerce.dto.CreateOrderItemRequest;
import br.com.ecommerce.dto.CreateOrderRequest;
import br.com.ecommerce.dto.OrderResponse;
import br.com.ecommerce.event.OrderEventPublisher;
import br.com.ecommerce.repository.OrderRepository;
import br.com.ecommerce.service.resilience.ProductCatalogGateway;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.math.BigDecimal;
import java.util.List;
import br.com.ecommerce.domain.OrderStatusChangeTrigger;
import br.com.ecommerce.repository.OrderStatusHistoryRepository;
import br.com.ecommerce.dto.OrderStatusHistoryResponse;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.WebApplicationException;
import org.jboss.resteasy.reactive.ClientWebApplicationException;

@ApplicationScoped
public class OrderService {

    @Inject
    OrderRepository orderRepository;

    @Inject
    ProductCatalogGateway productCatalogGateway;

    @Inject
    @RestClient
    DeliveryEstimateClient deliveryEstimateClient;

    @Inject
    OrderEventPublisher orderEventPublisher;

    @Inject
    OrderStateMachineService orderStateMachineService;

    @Inject
    OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        validateRequest(request);

        Order order = new Order();
        order.userId = request.userId();
        order.customerState = normalizeState(request.customerState());
        order.status = OrderStatus.CREATED;
        order.totalAmount = BigDecimal.ZERO;

        String originState = null;
        Integer totalItems = 0;

        for (CreateOrderItemRequest itemRequest : request.items()) {
            ProductClientResponse product = findProductOrThrow(itemRequest.productId());

            validateProductForOrder(product, itemRequest.quantity());

            if (originState == null) {
                originState = normalizeState(product.originState());
            } else if (!originState.equalsIgnoreCase(product.originState())) {
                throw new BadRequestException(
                        "Todos os produtos do pedido devem possuir o mesmo estado de origem nesta versão"
                );
            }

            OrderItem item = createOrderItem(product, itemRequest.quantity());

            order.addItem(item);
            order.totalAmount = order.totalAmount.add(item.totalPrice);
            totalItems += item.quantity;
        }

        DeliveryEstimateClientResponse deliveryEstimate = estimateDelivery(
                originState,
                order.customerState,
                totalItems
        );

        order.minDeliveryDays = deliveryEstimate.minDays();
        order.estimatedDeliveryDays = deliveryEstimate.estimatedDays();
        order.maxDeliveryDays = deliveryEstimate.maxDays();
        order.deliverySource = deliveryEstimate.source();
        order.deliveryModelVersion = deliveryEstimate.modelVersion();

        order.status = OrderStatus.CREATED;

        orderRepository.persistAndFlush(order);

        orderStateMachineService.transition(
                order,
                OrderStatus.WAITING_STOCK,
                OrderStatusChangeTrigger.ORDER_CREATED,
                "Pedido criado e aguardando reserva de estoque"
        );

        orderRepository.flush();

        orderEventPublisher.publishStockReservationRequested(order);
        return OrderResponse.fromEntity(order);
    }

    public List<OrderResponse> listAll() {
        return orderRepository
                .listAll(Sort.by("id").descending())
                .stream()
                .map(OrderResponse::fromEntity)
                .toList();
    }

    public List<OrderResponse> listByUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BadRequestException("ID do usuário inválido");
        }

        return orderRepository
                .listByUserId(userId)
                .stream()
                .map(OrderResponse::fromEntity)
                .toList();
    }

    public OrderResponse findById(Long id) {
        Order order = getOrderOrThrow(id);
        return OrderResponse.fromEntity(order);
    }

    @Transactional
    public OrderResponse cancel(Long id) {
        Order order = getOrderOrThrow(id);

        if (!order.canBeCanceled()) {
            throw new BadRequestException("O pedido não pode ser cancelado no status atual");
        }

        orderStateMachineService.transition(
            order,
            OrderStatus.CANCELED,
            OrderStatusChangeTrigger.ORDER_CANCELED,
            "Pedido cancelado"
        );

        orderRepository.flush();

        orderEventPublisher.publishOrderCanceled(order);

        return OrderResponse.fromEntity(order);
    }

    private void validateRequest(CreateOrderRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new BadRequestException("O pedido deve possuir pelo menos um item");
        }
    }

    private ProductClientResponse findProductOrThrow(Long productId) {
        try {
            return productCatalogGateway.getProductById(productId);

        } catch (ServiceUnavailableException exception) {
            throw exception;

        } catch (WebApplicationException exception) {
            int status = exception.getResponse() != null
                    ? exception.getResponse().getStatus()
                    : 0;

            if (status == 404) {
                throw new BadRequestException("Produto não encontrado: " + productId);
            }

            throw exception;
        }
    }

    private DeliveryEstimateClientResponse estimateDelivery(
            String originState,
            String destinationState,
            Integer totalItems
    ) {
        try {
            DeliveryEstimateClientRequest request = new DeliveryEstimateClientRequest(
                    originState,
                    destinationState,
                    totalItems
            );

            return deliveryEstimateClient.estimate(request);
        } catch (WebApplicationException exception) {
            throw new WebApplicationException(
                    "Falha ao consultar o delivery-estimator-api",
                    Response.Status.BAD_GATEWAY
            );
        } catch (Exception exception) {
            throw new WebApplicationException(
                    "Delivery-estimator-api indisponível no momento",
                    Response.Status.BAD_GATEWAY
            );
        }
    }

    private void validateProductForOrder(ProductClientResponse product, Integer quantity) {
        if (!product.isActive()) {
            throw new BadRequestException("Produto inativo: " + product.id());
        }

        if (quantity == null || quantity <= 0) {
            throw new BadRequestException("Quantidade inválida para o produto " + product.id());
        }
    }

    private OrderItem createOrderItem(ProductClientResponse product, Integer quantity) {
        OrderItem item = new OrderItem();

        item.productId = product.id();
        item.productName = product.name();
        item.productSku = product.sku();
        item.quantity = quantity;
        item.unitPrice = product.price();
        item.totalPrice = product.price().multiply(BigDecimal.valueOf(quantity));
        item.originState = normalizeState(product.originState());

        return item;
    }

    private Order getOrderOrThrow(Long id) {
        if (id == null || id <= 0) {
            throw new BadRequestException("ID do pedido inválido");
        }

        return orderRepository
                .findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Pedido não encontrado"));
    }

    private String normalizeState(String state) {
        if (state == null || state.isBlank()) {
            throw new BadRequestException("Estado é obrigatório");
        }

        return state.trim().toUpperCase();
    }
    @Transactional
    public OrderResponse approveFraud(Long orderId, BigDecimal riskScore, String reason) {
        Order order = getOrderOrThrow(orderId);

        if (OrderStatus.CANCELED.equals(order.status)) {
            return OrderResponse.fromEntity(order);
        }

        if (OrderStatus.REJECTED.equals(order.status)) {
            return OrderResponse.fromEntity(order);
        }

        order.fraudRiskScore = riskScore;
        order.fraudReason = reason;

        orderStateMachineService.transition(
                order,
                OrderStatus.CONFIRMED,
                OrderStatusChangeTrigger.FRAUD_APPROVED,
                reason
        );

        orderRepository.flush();

        return OrderResponse.fromEntity(order);
    }

    @Transactional
    public OrderResponse rejectFraud(Long orderId, BigDecimal riskScore, String reason) {
        Order order = getOrderOrThrow(orderId);

        if (OrderStatus.CANCELED.equals(order.status)) {
            return OrderResponse.fromEntity(order);
        }

        if (OrderStatus.CONFIRMED.equals(order.status)) {
            return OrderResponse.fromEntity(order);
        }

        order.fraudRiskScore = riskScore;
        order.fraudReason = reason;

        orderStateMachineService.transition(
                order,
                OrderStatus.REJECTED,
                OrderStatusChangeTrigger.FRAUD_REJECTED,
                reason
        );

        orderRepository.flush();

        return OrderResponse.fromEntity(order);
    }

    @Transactional
    public OrderResponse markStockReserved(Long orderId, String reason) {
        Order order = getOrderOrThrow(orderId);

        if (OrderStatus.CANCELED.equals(order.status)
                || OrderStatus.REJECTED.equals(order.status)
                || OrderStatus.CONFIRMED.equals(order.status)) {
            return OrderResponse.fromEntity(order);
        }

        order.stockReason = reason;

        orderStateMachineService.transition(
                order,
                OrderStatus.WAITING_FRAUD,
                OrderStatusChangeTrigger.STOCK_RESERVED,
                reason
        );

        orderRepository.flush();

        orderEventPublisher.publishOrderCreated(order);

        return OrderResponse.fromEntity(order);
    }

    @Transactional
    public OrderResponse markStockRejected(Long orderId, String reason) {
        Order order = getOrderOrThrow(orderId);

        if (OrderStatus.CANCELED.equals(order.status)
                || OrderStatus.CONFIRMED.equals(order.status)
                || OrderStatus.REJECTED.equals(order.status)) {
            return OrderResponse.fromEntity(order);
        }

        order.stockReason = reason;

        orderStateMachineService.transition(
                order,
                OrderStatus.REJECTED,
                OrderStatusChangeTrigger.STOCK_REJECTED,
                reason
        );

        orderRepository.flush();

        return OrderResponse.fromEntity(order);
    }

    public List<OrderStatusHistoryResponse> listStatusHistory(Long orderId) {
        getOrderOrThrow(orderId);

        return orderStatusHistoryRepository
                .listByOrderId(orderId)
                .stream()
                .map(OrderStatusHistoryResponse::fromEntity)
                .toList();
    }

}