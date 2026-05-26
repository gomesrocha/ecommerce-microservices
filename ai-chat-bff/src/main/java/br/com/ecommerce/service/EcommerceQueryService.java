package br.com.ecommerce.service;

import br.com.ecommerce.client.DeliveryClient;
import br.com.ecommerce.client.OrderClient;
import br.com.ecommerce.client.ProductClient;
import br.com.ecommerce.dto.EstimateDeliveryRequest;
import br.com.ecommerce.dto.EstimateDeliveryResponse;
import br.com.ecommerce.dto.OrderResponse;
import br.com.ecommerce.dto.ProductResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

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

        return """
                Pedido encontrado:
                - ID: %s
                - Usuário: %s
                - Estado do cliente: %s
                - Status: %s
                - Valor total: R$ %s
                - Prazo estimado: %s dias
                - Fonte da entrega: %s
                - Modelo da entrega: %s
                - Score de fraude: %s
                - Motivo da fraude: %s
                - Motivo do estoque: %s

                Esses dados foram consultados diretamente no order-api.
                """.formatted(
                order.id(),
                order.userId(),
                order.customerState(),
                order.status(),
                order.totalAmount(),
                order.estimatedDeliveryDays(),
                order.deliverySource(),
                order.deliveryModelVersion(),
                order.fraudRiskScore(),
                order.fraudReason(),
                order.stockReason()
        );
    }
}