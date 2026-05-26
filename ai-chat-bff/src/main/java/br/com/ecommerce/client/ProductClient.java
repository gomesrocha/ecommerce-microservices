package br.com.ecommerce.client;

import br.com.ecommerce.dto.ProductResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@Path("/products")
@RegisterRestClient(configKey = "product-api")
@Produces(MediaType.APPLICATION_JSON)
public interface ProductClient {

    @GET
    List<ProductResponse> listProducts();

    @GET
    @Path("/{id}")
    ProductResponse findById(@PathParam("id") Long id);
}