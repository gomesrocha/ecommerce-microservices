package br.com.ecommerce.client;

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
public interface DeliveryEstimateClient {

    @POST
    @Path("/estimate")
    DeliveryEstimateClientResponse estimate(DeliveryEstimateClientRequest request);
}