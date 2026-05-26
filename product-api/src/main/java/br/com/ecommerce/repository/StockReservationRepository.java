package br.com.ecommerce.repository;

import br.com.ecommerce.domain.StockReservation;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class StockReservationRepository implements PanacheRepository<StockReservation> {

    public Optional<StockReservation> findByOrderIdAndProductId(Long orderId, Long productId) {
        return find("orderId = ?1 and productId = ?2", orderId, productId)
                .firstResultOptional();
    }

    public List<StockReservation> listByOrderId(Long orderId) {
        return list("orderId", orderId);
    }
}