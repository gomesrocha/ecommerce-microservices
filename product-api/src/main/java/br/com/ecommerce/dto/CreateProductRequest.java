package br.com.ecommerce.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateProductRequest(

    @NotBlank(message = "O nome do produto é obrigatório")
    @Size(max = 120, message = "O nome do produto deve ter no máximo 120 caracteres")
    String name,

    @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres")
    String description,

    @NotBlank(message = "O SKU é obrigatório")
    @Size(max = 80, message = "O SKU deve ter no máximo 80 caracteres")
    String sku,

    @NotNull(message = "O preço é obrigatório")
    @Positive(message = "O preço deve ser maior que zero")
    BigDecimal price,

    @NotNull(message = "A quantidade em estoque é obrigatória")
    @PositiveOrZero(message = "A quantidade em estoque não pode ser negativa")
    Integer stockQuantity,

    @NotBlank(message = "O estado de origem é obrigatório")
    @Pattern(regexp = "^[A-Za-z]{2}$", message = "O estado de origem deve ter 2 letras. Exemplo: SP")
    String originState
) {
}