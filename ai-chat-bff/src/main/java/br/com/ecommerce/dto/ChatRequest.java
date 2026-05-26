package br.com.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank(message = "A mensagem é obrigatória")
        String message
) {
}