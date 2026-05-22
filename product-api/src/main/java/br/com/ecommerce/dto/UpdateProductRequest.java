package br.com.ecommerce.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateProductRequest(

    @Size(max = 120, message = "O nome do produto deve ter no máximo 120 caracteres")
    String name,

    @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres")
    String description,

    @Size(max = 80, message = "O SKU deve ter no máximo 80 caracteres")
    String sku,

    @Positive(message = "O preço deve ser maior que zero")
    BigDecimal price,

    @PositiveOrZero(message = "A quantidade em estoque não pode ser negativa")
    Integer stockQuantity,

    @Pattern(regexp = "^[A-Za-z]{2}$", message = "O estado de origem deve ter 2 letras. Exemplo: SP")
    String originState
) {
}