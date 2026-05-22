package br.com.ecommerce.repository;

import br.com.ecommerce.domain.Product;
import br.com.ecommerce.domain.ProductStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ProductRepository implements PanacheRepository<Product> {

    public Optional<Product> findBySku(String sku) {
        return find("sku", sku).firstResultOptional();
    }

    public boolean existsBySku(String sku) {
        return count("sku", sku) > 0;
    }

    public List<Product> listActive() {
        return list("status", ProductStatus.ACTIVE);
    }
}