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
import br.com.ecommerce.repository.OrderRepository;
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

@ApplicationScoped
public class OrderService {

    @Inject
    OrderRepository orderRepository;

    @Inject
    @RestClient
    ProductClient productClient;

    @Inject
    @RestClient
    DeliveryEstimateClient deliveryEstimateClient;

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

        order.status = OrderStatus.CONFIRMED;

        orderRepository.persist(order);

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

        order.status = OrderStatus.CANCELED;

        return OrderResponse.fromEntity(order);
    }

    private void validateRequest(CreateOrderRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new BadRequestException("O pedido deve possuir pelo menos um item");
        }
    }

    private ProductClientResponse findProductOrThrow(Long productId) {
        try {
            return productClient.findById(productId);
        } catch (WebApplicationException exception) {
            if (exception.getResponse() != null
                    && exception.getResponse().getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                throw new NotFoundException("Produto não encontrado: " + productId);
            }

            throw new WebApplicationException(
                    "Falha ao consultar o product-api",
                    Response.Status.BAD_GATEWAY
            );
        } catch (Exception exception) {
            throw new WebApplicationException(
                    "Product-api indisponível no momento",
                    Response.Status.BAD_GATEWAY
            );
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

        if (!product.hasStock(quantity)) {
            throw new BadRequestException(
                    "Estoque insuficiente para o produto " + product.id()
            );
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
}