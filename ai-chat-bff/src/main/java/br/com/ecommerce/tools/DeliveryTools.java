package br.com.ecommerce.tools;

import br.com.ecommerce.client.DeliveryClient;
import br.com.ecommerce.dto.EstimateDeliveryRequest;
import br.com.ecommerce.dto.EstimateDeliveryResponse;
import dev.langchain4j.agent.tool.Tool;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class DeliveryTools {

    @Inject
    @RestClient
    DeliveryClient deliveryClient;

    @Tool("Estima o prazo de entrega a partir do estado de origem, estado de destino e quantidade total de itens")
    public EstimateDeliveryResponse estimarEntrega(String originState, String destinationState, Integer totalItems) {
        return deliveryClient.estimate(new EstimateDeliveryRequest(
                originState,
                destinationState,
                totalItems
        ));
    }
}