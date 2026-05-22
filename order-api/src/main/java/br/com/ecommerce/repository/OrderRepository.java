package br.com.ecommerce.repository;

import br.com.ecommerce.domain.Order;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class OrderRepository implements PanacheRepository<Order> {

    public List<Order> listByUserId(Long userId) {
        return list("userId", userId);
    }
}