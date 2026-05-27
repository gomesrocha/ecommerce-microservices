package br.com.ecommerce.client;

import br.com.ecommerce.dto.PaymentGatewayAuthorizeRequest;
import br.com.ecommerce.dto.PaymentGatewayAuthorizeResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/payments")
@RegisterRestClient(configKey = "payment-gateway")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface PaymentGatewayClient {

    @POST
    @Path("/authorize")
    PaymentGatewayAuthorizeResponse authorize(PaymentGatewayAuthorizeRequest request);
}