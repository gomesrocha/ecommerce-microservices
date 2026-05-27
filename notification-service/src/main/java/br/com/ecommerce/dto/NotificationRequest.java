package br.com.ecommerce.dto;

import br.com.ecommerce.domain.NotificationChannel;
import br.com.ecommerce.domain.NotificationSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Map;

public record NotificationRequest(
        String eventId,

        @NotBlank(message = "O tipo do evento é obrigatório")
        String eventType,

        String aggregateType,
        Long aggregateId,
        Long userId,

        @NotBlank(message = "O título é obrigatório")
        String title,

        @NotBlank(message = "A mensagem é obrigatória")
        String message,

        String email,

        NotificationSeverity severity,

        @NotEmpty(message = "Informe pelo menos um canal")
        List<NotificationChannel> channels,

        Map<String, Object> metadata
) {
}