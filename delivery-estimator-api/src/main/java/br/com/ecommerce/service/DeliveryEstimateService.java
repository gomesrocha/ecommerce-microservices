package br.com.ecommerce.service;

import br.com.ecommerce.domain.DeliveryRouteEstimate;
import br.com.ecommerce.dto.DeliveryRouteResponse;
import br.com.ecommerce.dto.EstimateDeliveryRequest;
import br.com.ecommerce.dto.EstimateDeliveryResponse;
import br.com.ecommerce.dto.UpsertDeliveryRouteRequest;
import br.com.ecommerce.ml.DeliveryPredictionResult;
import br.com.ecommerce.ml.DeliveryTribuoModelService;
import br.com.ecommerce.repository.DeliveryRouteEstimateRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class DeliveryEstimateService {

    @Inject
    DeliveryRouteEstimateRepository repository;

    @Inject
    DeliveryTribuoModelService tribuoModelService;

    @ConfigProperty(name = "app.delivery-ml.prefer-model", defaultValue = "true")
    boolean preferModel;

    public EstimateDeliveryResponse estimate(EstimateDeliveryRequest request) {
        String origin = normalizeState(request.originState());
        String destination = normalizeState(request.destinationState());
        Integer totalItems = validateTotalItems(request.totalItems());

        if (preferModel) {
            Optional<EstimateDeliveryResponse> modelResponse =
                    estimateWithModel(origin, destination, totalItems);

            if (modelResponse.isPresent()) {
                return modelResponse.get();
            }
        }

        Optional<EstimateDeliveryResponse> routeResponse =
                estimateWithPersistedRoute(origin, destination, totalItems);

        if (routeResponse.isPresent()) {
            return routeResponse.get();
        }

        if (!preferModel) {
            Optional<EstimateDeliveryResponse> modelResponse =
                    estimateWithModel(origin, destination, totalItems);

            if (modelResponse.isPresent()) {
                return modelResponse.get();
            }
        }

        return estimateWithFallback(origin, destination, totalItems);
    }

    public List<DeliveryRouteResponse> listRoutes() {
        return repository
                .listAll(Sort.by("originState").and("destinationState"))
                .stream()
                .map(DeliveryRouteResponse::fromEntity)
                .toList();
    }

    @Transactional
    public DeliveryRouteResponse upsertRoute(UpsertDeliveryRouteRequest request) {
        validateDays(request.minDays(), request.estimatedDays(), request.maxDays());

        String origin = normalizeState(request.originState());
        String destination = normalizeState(request.destinationState());

        DeliveryRouteEstimate route = repository
                .findByOriginAndDestination(origin, destination)
                .orElseGet(DeliveryRouteEstimate::new);

        route.originState = origin;
        route.destinationState = destination;
        route.minDays = request.minDays();
        route.estimatedDays = request.estimatedDays();
        route.maxDays = request.maxDays();
        route.source = normalizeOrDefault(request.source(), "MANUAL_BASELINE");
        route.modelVersion = normalizeOrDefault(request.modelVersion(), "baseline-routes-v1");

        if (route.id == null) {
            repository.persist(route);
        }

        return DeliveryRouteResponse.fromEntity(route);
    }

    private Optional<EstimateDeliveryResponse> estimateWithModel(
            String origin,
            String destination,
            Integer totalItems
    ) {
        if (!tribuoModelService.isReady()) {
            return Optional.empty();
        }

        return tribuoModelService
                .predict(origin, destination, totalItems)
                .map(result -> toResponse(origin, destination, totalItems, result));
    }

    private EstimateDeliveryResponse toResponse(
            String origin,
            String destination,
            Integer totalItems,
            DeliveryPredictionResult result
    ) {
        return new EstimateDeliveryResponse(
                origin,
                destination,
                totalItems,
                result.minDays(),
                result.estimatedDays(),
                result.maxDays(),
                result.source(),
                result.modelVersion()
        );
    }

    private Optional<EstimateDeliveryResponse> estimateWithPersistedRoute(
            String origin,
            String destination,
            Integer totalItems
    ) {
        return repository
                .findByOriginAndDestination(origin, destination)
                .map(route -> {
                    Integer extraDaysByVolume = calculateExtraDaysByVolume(totalItems);

                    return new EstimateDeliveryResponse(
                            origin,
                            destination,
                            totalItems,
                            route.minDays + extraDaysByVolume,
                            route.estimatedDays + extraDaysByVolume,
                            route.maxDays + extraDaysByVolume,
                            route.source,
                            route.modelVersion
                    );
                });
    }

    private EstimateDeliveryResponse estimateWithFallback(
            String origin,
            String destination,
            Integer totalItems
    ) {
        DeliveryRouteEstimate route = createFallbackRoute(origin, destination);
        Integer extraDaysByVolume = calculateExtraDaysByVolume(totalItems);

        return new EstimateDeliveryResponse(
                origin,
                destination,
                totalItems,
                route.minDays + extraDaysByVolume,
                route.estimatedDays + extraDaysByVolume,
                route.maxDays + extraDaysByVolume,
                route.source,
                route.modelVersion
        );
    }

    private DeliveryRouteEstimate createFallbackRoute(String origin, String destination) {
        DeliveryRouteEstimate route = new DeliveryRouteEstimate();

        route.originState = origin;
        route.destinationState = destination;

        if (origin.equals(destination)) {
            route.minDays = 1;
            route.estimatedDays = 2;
            route.maxDays = 4;
        } else {
            route.minDays = 4;
            route.estimatedDays = 7;
            route.maxDays = 12;
        }

        route.source = "FALLBACK_RULE";
        route.modelVersion = "fallback-v1";

        return route;
    }

    private Integer validateTotalItems(Integer totalItems) {
        if (totalItems == null || totalItems <= 0) {
            throw new BadRequestException("A quantidade de itens deve ser maior que zero");
        }

        return totalItems;
    }

    private Integer calculateExtraDaysByVolume(Integer totalItems) {
        validateTotalItems(totalItems);

        if (totalItems <= 3) {
            return 0;
        }

        if (totalItems <= 10) {
            return 1;
        }

        return 2;
    }

    private void validateDays(Integer minDays, Integer estimatedDays, Integer maxDays) {
        if (minDays > estimatedDays) {
            throw new BadRequestException("O prazo mínimo não pode ser maior que o prazo estimado");
        }

        if (estimatedDays > maxDays) {
            throw new BadRequestException("O prazo estimado não pode ser maior que o prazo máximo");
        }
    }

    private String normalizeState(String state) {
        if (state == null || state.isBlank()) {
            throw new BadRequestException("Estado é obrigatório");
        }

        return state.trim().toUpperCase();
    }

    private String normalizeOrDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return value.trim();
    }
}