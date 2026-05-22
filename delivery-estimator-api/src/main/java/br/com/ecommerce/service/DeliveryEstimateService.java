package br.com.ecommerce.service;

import br.com.ecommerce.domain.DeliveryRouteEstimate;
import br.com.ecommerce.dto.DeliveryRouteResponse;
import br.com.ecommerce.dto.EstimateDeliveryRequest;
import br.com.ecommerce.dto.EstimateDeliveryResponse;
import br.com.ecommerce.dto.UpsertDeliveryRouteRequest;
import br.com.ecommerce.repository.DeliveryRouteEstimateRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;

import java.util.List;

@ApplicationScoped
public class DeliveryEstimateService {

    @Inject
    DeliveryRouteEstimateRepository repository;

    public EstimateDeliveryResponse estimate(EstimateDeliveryRequest request) {
        String origin = normalizeState(request.originState());
        String destination = normalizeState(request.destinationState());
        Integer totalItems = request.totalItems();

        DeliveryRouteEstimate route = repository
                .findByOriginAndDestination(origin, destination)
                .orElseGet(() -> createFallbackRoute(origin, destination));

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

    private Integer calculateExtraDaysByVolume(Integer totalItems) {
        if (totalItems == null || totalItems <= 0) {
            throw new BadRequestException("A quantidade de itens deve ser maior que zero");
        }

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