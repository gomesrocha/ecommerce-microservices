package br.com.ecommerce.resource;

import br.com.ecommerce.dto.CreateProductRequest;
import br.com.ecommerce.dto.ProductResponse;
import br.com.ecommerce.dto.UpdateProductRequest;
import br.com.ecommerce.service.ProductService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Path("/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductResource {

    @Inject
    ProductService productService;

    @POST
    public Response create(@Valid CreateProductRequest request) {
        ProductResponse response = productService.create(request);

        return Response
            .created(URI.create("/products/" + response.id()))
            .entity(response)
            .build();
    }

    @GET
    public List<ProductResponse> listAll(@QueryParam("activeOnly") @DefaultValue("false") boolean activeOnly) {
        if (activeOnly) {
            return productService.listActive();
        }

        return productService.listAll();
    }

    @GET
    @Path("/{id}")
    public ProductResponse findById(@PathParam("id") Long id) {
        return productService.findById(id);
    }

    @GET
    @Path("/sku/{sku}")
    public ProductResponse findBySku(@PathParam("sku") String sku) {
        return productService.findBySku(sku);
    }

    @PUT
    @Path("/{id}")
    public ProductResponse update(
        @PathParam("id") Long id,
        @Valid UpdateProductRequest request
    ) {
        return productService.update(id, request);
    }

    @PATCH
    @Path("/{id}/deactivate")
    public ProductResponse deactivate(@PathParam("id") Long id) {
        return productService.deactivate(id);
    }

    @GET
    @Path("/{id}/availability")
    public Map<String, Object> checkAvailability(
        @PathParam("id") Long id,
        @QueryParam("quantity") @DefaultValue("1") Integer quantity
    ) {
        return productService.checkAvailability(id, quantity);
    }
}