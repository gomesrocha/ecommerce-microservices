package br.com.ecommerce.repository;

import br.com.ecommerce.domain.OutboxEvent;
import br.com.ecommerce.domain.OutboxStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class OutboxEventRepository implements PanacheRepository<OutboxEvent> {

    public List<OutboxEvent> listPending(int limit) {
        return find("status = ?1 order by createdAt asc", OutboxStatus.PENDING)
                .page(0, limit)
                .list();
    }
}