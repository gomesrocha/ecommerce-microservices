package br.com.ecommerce.repository;

import br.com.ecommerce.domain.OrderStatusHistory;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class OrderStatusHistoryRepository implements PanacheRepository<OrderStatusHistory> {

    public List<OrderStatusHistory> listByOrderId(Long orderId) {
        return list("orderId", orderId);
    }
}