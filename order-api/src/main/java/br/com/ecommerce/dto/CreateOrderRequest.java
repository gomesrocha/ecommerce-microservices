package br.com.ecommerce.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record CreateOrderRequest(

        @NotNull(message = "O ID do usuário é obrigatório")
        Long userId,

        @NotBlank(message = "O estado do cliente é obrigatório")
        @Pattern(regexp = "^[A-Za-z]{2}$", message = "O estado do cliente deve ter 2 letras. Exemplo: SE")
        String customerState,

        @Valid
        @NotEmpty(message = "O pedido deve possuir pelo menos um item")
        List<CreateOrderItemRequest> items
) {
}