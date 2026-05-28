package br.com.ecommerce.resource;

import br.com.ecommerce.dto.FraudAnalysisResponse;
import br.com.ecommerce.service.FraudAnalysisService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import br.com.ecommerce.mlops.ModelInfoResponse;
import br.com.ecommerce.mlops.ModelMetadataService;

@Path("/fraud-analyses")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FraudAnalysisResource {

    @Inject
    FraudAnalysisService service;

    @Inject
    ModelMetadataService modelMetadataService;

    @GET
    @Path("/model-info")
    public ModelInfoResponse modelInfo() {
        return modelMetadataService.getModelInfo();
    }

    @GET
    public List<FraudAnalysisResponse> listAll() {
        return service.listAll()
                .stream()
                .map(FraudAnalysisResponse::fromEntity)
                .toList();
    }

    @GET
    @Path("/order/{orderId}")
    public FraudAnalysisResponse findByOrderId(@PathParam("orderId") Long orderId) {
        return FraudAnalysisResponse.fromEntity(service.findByOrderId(orderId));
    }
}