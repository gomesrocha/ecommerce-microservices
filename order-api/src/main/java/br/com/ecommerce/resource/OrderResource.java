package br.com.ecommerce.resource;

import br.com.ecommerce.dto.CreateOrderRequest;
import br.com.ecommerce.dto.OrderResponse;
import br.com.ecommerce.service.OrderService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.List;
import br.com.ecommerce.dto.OrderStatusHistoryResponse;

@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderResource {

    @Inject
    OrderService orderService;

    @POST
    public Response create(@Valid CreateOrderRequest request) {
        OrderResponse response = orderService.create(request);

        return Response
                .created(URI.create("/orders/" + response.id()))
                .entity(response)
                .build();
    }

    @GET
    public List<OrderResponse> listAll(@QueryParam("userId") Long userId) {
        if (userId != null) {
            return orderService.listByUserId(userId);
        }

        return orderService.listAll();
    }

    @GET
    @Path("/{id}/history")
    public List<OrderStatusHistoryResponse> listStatusHistory(@PathParam("id") Long id) {
        return orderService.listStatusHistory(id);
    }

    @GET
    @Path("/{id}")
    public OrderResponse findById(@PathParam("id") Long id) {
        return orderService.findById(id);
    }



    @PATCH
    @Path("/{id}/cancel")
    public OrderResponse cancel(@PathParam("id") Long id) {
        return orderService.cancel(id);
    }
}