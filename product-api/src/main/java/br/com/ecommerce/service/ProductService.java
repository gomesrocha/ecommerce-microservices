package br.com.ecommerce.service;

import br.com.ecommerce.domain.Product;
import br.com.ecommerce.domain.ProductStatus;
import br.com.ecommerce.dto.CreateProductRequest;
import br.com.ecommerce.dto.ProductResponse;
import br.com.ecommerce.dto.UpdateProductRequest;
import br.com.ecommerce.repository.ProductRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ProductService {

    @Inject
    ProductRepository productRepository;

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        String normalizedSku = normalizeSku(request.sku());

        if (productRepository.existsBySku(normalizedSku)) {
            throw new WebApplicationException(
                "Já existe um produto cadastrado com o SKU informado",
                Response.Status.CONFLICT
            );
        }

        Product product = new Product();
        product.name = request.name().trim();
        product.description = normalizeNullableText(request.description());
        product.sku = normalizedSku;
        product.price = request.price();
        product.stockQuantity = request.stockQuantity();
        product.originState = normalizeState(request.originState());
        product.status = ProductStatus.ACTIVE;

        productRepository.persist(product);

        return ProductResponse.fromEntity(product);
    }

    public List<ProductResponse> listAll() {
        return productRepository
            .listAll(Sort.by("id").descending())
            .stream()
            .map(ProductResponse::fromEntity)
            .toList();
    }

    public List<ProductResponse> listActive() {
        return productRepository
            .listActive()
            .stream()
            .map(ProductResponse::fromEntity)
            .toList();
    }

    public ProductResponse findById(Long id) {
        Product product = getProductOrThrow(id);
        return ProductResponse.fromEntity(product);
    }

    public ProductResponse findBySku(String sku) {
        Product product = productRepository
            .findBySku(normalizeSku(sku))
            .orElseThrow(() -> new NotFoundException("Produto não encontrado para o SKU informado"));

        return ProductResponse.fromEntity(product);
    }

    @Transactional
    public ProductResponse update(Long id, UpdateProductRequest request) {
        Product product = getProductOrThrow(id);

        if (request.sku() != null && !request.sku().isBlank()) {
            String normalizedSku = normalizeSku(request.sku());

            productRepository
                .findBySku(normalizedSku)
                .filter(existingProduct -> !existingProduct.id.equals(id))
                .ifPresent(existingProduct -> {
                    throw new WebApplicationException(
                        "Já existe outro produto cadastrado com o SKU informado",
                        Response.Status.CONFLICT
                    );
                });

            product.sku = normalizedSku;
        }

        if (request.name() != null && !request.name().isBlank()) {
            product.name = request.name().trim();
        }

        if (request.description() != null) {
            product.description = normalizeNullableText(request.description());
        }

        if (request.price() != null) {
            product.price = request.price();
        }

        if (request.stockQuantity() != null) {
            product.stockQuantity = request.stockQuantity();
        }

        if (request.originState() != null && !request.originState().isBlank()) {
            product.originState = normalizeState(request.originState());
        }

        return ProductResponse.fromEntity(product);
    }

    @Transactional
    public ProductResponse deactivate(Long id) {
        Product product = getProductOrThrow(id);
        product.status = ProductStatus.INACTIVE;

        return ProductResponse.fromEntity(product);
    }

    public Map<String, Object> checkAvailability(Long id, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BadRequestException("A quantidade deve ser maior que zero");
        }

        Product product = getProductOrThrow(id);

        boolean available = product.isActive() && product.hasStock(quantity);

        return Map.of(
            "productId", product.id,
            "sku", product.sku,
            "requestedQuantity", quantity,
            "availableStock", product.stockQuantity,
            "active", product.isActive(),
            "available", available
        );
    }

    private Product getProductOrThrow(Long id) {
        if (id == null || id <= 0) {
            throw new BadRequestException("ID do produto inválido");
        }

        return productRepository
            .findByIdOptional(id)
            .orElseThrow(() -> new NotFoundException("Produto não encontrado"));
    }

    private String normalizeSku(String sku) {
        if (sku == null || sku.isBlank()) {
            throw new BadRequestException("SKU é obrigatório");
        }

        return sku.trim().toUpperCase();
    }

    private String normalizeState(String state) {
        if (state == null || state.isBlank()) {
            throw new BadRequestException("Estado de origem é obrigatório");
        }

        return state.trim().toUpperCase();
    }

    private String normalizeNullableText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}