package br.com.ecommerce.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items", schema = "orders")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    public Order order;

    @Column(name = "product_id", nullable = false)
    public Long productId;

    @Column(name = "product_name", nullable = false, length = 120)
    public String productName;

    @Column(name = "product_sku", nullable = false, length = 80)
    public String productSku;

    @Column(nullable = false)
    public Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    public BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    public BigDecimal totalPrice;

    @Column(name = "origin_state", nullable = false, length = 2)
    public String originState;
}