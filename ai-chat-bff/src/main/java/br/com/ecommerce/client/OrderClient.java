package br.com.ecommerce.client;

import br.com.ecommerce.dto.OrderResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/orders")
@RegisterRestClient(configKey = "order-api")
@Produces(MediaType.APPLICATION_JSON)
public interface OrderClient {

    @GET
    @Path("/{id}")
    OrderResponse findById(@PathParam("id") Long id);
}