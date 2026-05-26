package br.com.ecommerce.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/products")
@RegisterRestClient(configKey = "product-api")
@Produces(MediaType.APPLICATION_JSON)
public interface ProductClient {

    @GET
    @Path("/{id}")
    ProductClientResponse findById(@PathParam("id") Long id);
}