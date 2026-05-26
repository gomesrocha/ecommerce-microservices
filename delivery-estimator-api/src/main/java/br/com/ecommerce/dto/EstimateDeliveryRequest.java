package br.com.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record EstimateDeliveryRequest(

        @NotBlank(message = "O estado de origem é obrigatório")
        @Pattern(regexp = "^[A-Za-z]{2}$", message = "O estado de origem deve ter 2 letras. Exemplo: SP")
        String originState,

        @NotBlank(message = "O estado de destino é obrigatório")
        @Pattern(regexp = "^[A-Za-z]{2}$", message = "O estado de destino deve ter 2 letras. Exemplo: SE")
        String destinationState,

        @NotNull(message = "A quantidade de itens é obrigatória")
        @Positive(message = "A quantidade de itens deve ser maior que zero")
        Integer totalItems
) {
}