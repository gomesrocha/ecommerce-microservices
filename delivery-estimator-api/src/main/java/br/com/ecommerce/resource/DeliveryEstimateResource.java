package br.com.ecommerce.resource;

import br.com.ecommerce.dto.DeliveryRouteResponse;
import br.com.ecommerce.dto.EstimateDeliveryRequest;
import br.com.ecommerce.dto.EstimateDeliveryResponse;
import br.com.ecommerce.dto.UpsertDeliveryRouteRequest;
import br.com.ecommerce.metrics.DeliveryMetricsService;
import br.com.ecommerce.service.DeliveryEstimateService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/delivery-estimates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DeliveryEstimateResource {

    @Inject
    DeliveryEstimateService service;

    @Inject
    DeliveryMetricsService metricsService;

    @POST
    @Path("/estimate")
    public EstimateDeliveryResponse estimate(@Valid EstimateDeliveryRequest request) {
        EstimateDeliveryResponse response = service.estimate(request);
        metricsService.recordPrediction(response);
        return response;
    }

    @GET
    @Path("/routes")
    public List<DeliveryRouteResponse> listRoutes() {
        return service.listRoutes();
    }

    @PUT
    @Path("/routes")
    public DeliveryRouteResponse upsertRoute(@Valid UpsertDeliveryRouteRequest request) {
        return service.upsertRoute(request);
    }
}