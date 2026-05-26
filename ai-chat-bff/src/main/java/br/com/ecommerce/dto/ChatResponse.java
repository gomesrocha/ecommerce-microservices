package br.com.ecommerce.dto;

import java.time.LocalDateTime;

public record ChatResponse(
        String answer,
        String model,
        LocalDateTime createdAt
) {
}