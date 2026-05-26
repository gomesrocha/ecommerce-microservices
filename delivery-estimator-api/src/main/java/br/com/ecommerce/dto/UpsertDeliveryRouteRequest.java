package br.com.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record UpsertDeliveryRouteRequest(

        @NotBlank(message = "O estado de origem é obrigatório")
        @Pattern(regexp = "^[A-Za-z]{2}$", message = "O estado de origem deve ter 2 letras. Exemplo: SP")
        String originState,

        @NotBlank(message = "O estado de destino é obrigatório")
        @Pattern(regexp = "^[A-Za-z]{2}$", message = "O estado de destino deve ter 2 letras. Exemplo: SE")
        String destinationState,

        @NotNull(message = "O prazo mínimo é obrigatório")
        @Positive(message = "O prazo mínimo deve ser maior que zero")
        Integer minDays,

        @NotNull(message = "O prazo estimado é obrigatório")
        @Positive(message = "O prazo estimado deve ser maior que zero")
        Integer estimatedDays,

        @NotNull(message = "O prazo máximo é obrigatório")
        @Positive(message = "O prazo máximo deve ser maior que zero")
        Integer maxDays,

        String source,

        String modelVersion
) {
}