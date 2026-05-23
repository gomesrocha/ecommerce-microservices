package br.com.ecommerce.repository;

import br.com.ecommerce.domain.FraudAnalysis;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class FraudAnalysisRepository implements PanacheRepository<FraudAnalysis> {

    public Optional<FraudAnalysis> findByOrderId(Long orderId) {
        return find("orderId", orderId).firstResultOptional();
    }

    public List<FraudAnalysis> listByUserId(Long userId) {
        return list("userId", userId);
    }
}