package br.com.ecommerce.resource;

import br.com.ecommerce.dto.PaymentRequestedEvent;
import br.com.ecommerce.dto.PaymentResponse;
import br.com.ecommerce.service.PaymentService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/payments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PaymentResource {

    @Inject
    PaymentService service;

    @GET
    public List<PaymentResponse> listAll() {
        return service.listAll();
    }

    @GET
    @Path("/{id}")
    public PaymentResponse findById(@PathParam("id") Long id) {
        return service.findById(id);
    }

    @GET
    @Path("/order/{orderId}")
    public PaymentResponse findByOrderId(@PathParam("orderId") Long orderId) {
        return service.findByOrderId(orderId);
    }

    @POST
    @Path("/simulate")
    public PaymentResponse simulate(@Valid PaymentRequestedEvent event) {
        return service.process(event);
    }
}