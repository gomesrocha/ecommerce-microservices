package br.com.ecommerce.repository;

import br.com.ecommerce.domain.Payment;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class PaymentRepository implements PanacheRepository<Payment> {

    public Optional<Payment> findByOrderId(Long orderId) {
        return find("orderId", orderId).firstResultOptional();
    }

    public Optional<Payment> findByEventId(String eventId) {
        return find("eventId", eventId).firstResultOptional();
    }
}