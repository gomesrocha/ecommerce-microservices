package br.com.ecommerce.tools;

import br.com.ecommerce.client.OrderClient;
import br.com.ecommerce.dto.OrderResponse;
import dev.langchain4j.agent.tool.Tool;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class OrderTools {

    @Inject
    @RestClient
    OrderClient orderClient;

    @Tool("Consulta o status e os dados de um pedido pelo ID")
    public OrderResponse consultarPedido(Long orderId) {
        return orderClient.findById(orderId);
    }
}