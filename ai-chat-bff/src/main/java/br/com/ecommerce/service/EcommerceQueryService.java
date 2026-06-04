package br.com.ecommerce.service;

import br.com.ecommerce.client.DeliveryClient;
import br.com.ecommerce.client.OrderClient;
import br.com.ecommerce.client.ProductClient;
import br.com.ecommerce.dto.CreateOrderItemRequest;
import br.com.ecommerce.dto.CreateOrderRequest;
import br.com.ecommerce.dto.EstimateDeliveryRequest;
import br.com.ecommerce.dto.EstimateDeliveryResponse;
import br.com.ecommerce.dto.OrderResponse;
import br.com.ecommerce.dto.ProductResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class EcommerceQueryService {
    private static final Logger LOG = Logger.getLogger(EcommerceQueryService.class);

    @Inject
    @RestClient
    ProductClient productClient;

    @Inject
    @RestClient
    DeliveryClient deliveryClient;

    @Inject
    @RestClient
    OrderClient orderClient;

    public String listProducts() {
        List<ProductResponse> products = productClient.listProducts();

        if (products == null || products.isEmpty()) {
            return "Não encontrei produtos cadastrados no ecommerce.";
        }

        return products.stream()
                .limit(10)
                .map(product -> "- ID " + product.id()
                        + " | " + product.name()
                        + " | SKU " + product.sku()
                        + " | Preço R$ " + product.price()
                        + " | Estoque " + product.stockQuantity()
                        + " | Origem " + product.originState())
                .collect(Collectors.joining(
                        "\n",
                        "Produtos disponíveis:\n",
                        "\n\nEsses dados foram consultados diretamente no product-api."
                ));
    }

    public String getProduct(Long productId) {
        ProductResponse product = productClient.findById(productId);

        return """
                Produto encontrado:
                - ID: %s
                - Nome: %s
                - SKU: %s
                - Preço: R$ %s
                - Estoque: %s
                - Origem: %s
                - Status: %s

                Esses dados foram consultados diretamente no product-api.
                """.formatted(
                product.id(),
                product.name(),
                product.sku(),
                product.price(),
                product.stockQuantity(),
                product.originState(),
                product.status()
        );
    }

    public String estimateDelivery(String originState, String destinationState, Integer totalItems) {
        EstimateDeliveryResponse response = deliveryClient.estimate(
                new EstimateDeliveryRequest(originState, destinationState, totalItems)
        );

        return """
                Estimativa de entrega:
                - Origem: %s
                - Destino: %s
                - Quantidade de itens: %s
                - Prazo mínimo: %s dias
                - Prazo estimado: %s dias
                - Prazo máximo: %s dias
                - Fonte: %s
                - Versão do modelo: %s

                Essa estimativa foi consultada diretamente no delivery-estimator-api.
                """.formatted(
                response.originState(),
                response.destinationState(),
                response.totalItems(),
                response.minDays(),
                response.estimatedDays(),
                response.maxDays(),
                response.source(),
                response.modelVersion()
        );
    }

    public String getOrder(Long orderId) {
        OrderResponse order = orderClient.findById(orderId);

        return formatOrder(order, "Pedido encontrado:");
    }

    public String createOrderWithExplicitConfirmation(
            ChatIntentService.OrderDraft draft,
            Long userId,
            String correlationId
    ) {
        ProductResponse product = productClient.findById(draft.productId());

        int quantity = draft.quantity() == null || draft.quantity() <= 0
                ? 1
                : draft.quantity();

        if (!"ACTIVE".equalsIgnoreCase(product.status())) {
            return """
                    Não posso criar o pedido porque o produto não está ativo.

                    Produto:
                    - ID: %s
                    - Nome: %s
                    - Status: %s
                    """.formatted(product.id(), product.name(), product.status());
        }

        if (product.stockQuantity() == null || product.stockQuantity() < quantity) {
            return """
                    Não posso criar o pedido porque não há estoque suficiente.

                    Produto:
                    - ID: %s
                    - Nome: %s
                    - Estoque atual: %s
                    - Quantidade solicitada: %s
                    """.formatted(product.id(), product.name(), product.stockQuantity(), quantity);
        }

        BigDecimal total = product.price().multiply(BigDecimal.valueOf(quantity));

        if (!draft.confirmed()) {
            return """
                    Encontrei os dados para criação do pedido, mas ainda não vou criar automaticamente.

                    Resumo do pedido:
                    - Produto: %s
                    - ID do produto: %s
                    - SKU: %s
                    - Quantidade: %s
                    - Preço unitário: R$ %s
                    - Total estimado: R$ %s
                    - Estado de entrega: %s

                    Para confirmar a criação, envie exatamente uma mensagem como:
                    "Confirmo criar pedido do produto %s com %s item para %s"
                    """.formatted(
                    product.name(),
                    product.id(),
                    product.sku(),
                    quantity,
                    product.price(),
                    total,
                    draft.customerState(),
                    product.id(),
                    quantity,
                    draft.customerState()
            );
        }

        CreateOrderRequest request = new CreateOrderRequest(
                userId,
                draft.customerState(),
                List.of(new CreateOrderItemRequest(product.id(), quantity))
        );

        OrderResponse order = orderClient.create(request, correlationId);

        LOG.infof(
                "Pedido criado via ai-chat-bff. orderId=%s, productId=%s, quantity=%s, userId=%s, correlationId=%s",
                order.id(),
                product.id(),
                quantity,
                userId,
                correlationId
        );

        return formatOrder(order, """
                Pedido criado com sucesso via AI Chat BFF.

                O pedido foi criado somente após confirmação explícita do usuário.
                """);
    }

    private String formatOrder(OrderResponse order, String title) {
        return """
                %s

                - ID: %s
                - Usuário: %s
                - Estado do cliente: %s
                - Status: %s
                - Valor total: R$ %s
                - Prazo mínimo: %s dias
                - Prazo estimado: %s dias
                - Prazo máximo: %s dias
                - Fonte da entrega: %s
                - Modelo da entrega: %s
                - Score de fraude: %s
                - Motivo da fraude: %s
                - Motivo do estoque: %s

                Esses dados foram consultados diretamente no order-api.
                """.formatted(
                title,
                order.id(),
                order.userId(),
                order.customerState(),
                order.status(),
                order.totalAmount(),
                order.minDeliveryDays(),
                order.estimatedDeliveryDays(),
                order.maxDeliveryDays(),
                order.deliverySource(),
                order.deliveryModelVersion(),
                order.fraudRiskScore(),
                order.fraudReason(),
                order.stockReason()
        );
    }
}
