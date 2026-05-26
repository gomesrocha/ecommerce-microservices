package br.com.ecommerce.client;

import br.com.ecommerce.dto.EstimateDeliveryRequest;
import br.com.ecommerce.dto.EstimateDeliveryResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/delivery-estimates")
@RegisterRestClient(configKey = "delivery-estimator-api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface DeliveryClient {

    @POST
    @Path("/estimate")
    EstimateDeliveryResponse estimate(EstimateDeliveryRequest request);
}