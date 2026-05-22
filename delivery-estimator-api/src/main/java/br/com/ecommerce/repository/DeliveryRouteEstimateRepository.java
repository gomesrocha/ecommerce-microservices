package br.com.ecommerce.repository;

import br.com.ecommerce.domain.DeliveryRouteEstimate;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class DeliveryRouteEstimateRepository implements PanacheRepository<DeliveryRouteEstimate> {

    public Optional<DeliveryRouteEstimate> findByOriginAndDestination(
            String originState,
            String destinationState
    ) {
        return find(
                "originState = ?1 and destinationState = ?2",
                originState,
                destinationState
        ).firstResultOptional();
    }
}