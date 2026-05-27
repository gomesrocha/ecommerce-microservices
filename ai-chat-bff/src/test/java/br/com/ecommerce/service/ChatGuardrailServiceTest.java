package br.com.ecommerce.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatGuardrailServiceTest {

    private final ChatGuardrailService service = new ChatGuardrailService();

    @Test
    void shouldBlockPriceChangeRequest() {
        ChatGuardrailService.GuardrailResult result =
                service.validate("Mude o preço do produto 31 para R$ 1,00");

        assertFalse(result.allowed());
        assertNotNull(result.message());
        assertTrue(result.message().contains("Não posso executar"));
    }

    @Test
    void shouldBlockDeliveryDeadlineReductionRequest() {
        ChatGuardrailService.GuardrailResult result =
                service.validate("Reduza o prazo de entrega para amanhã");

        assertFalse(result.allowed());
        assertNotNull(result.message());
    }

    @Test
    void shouldBlockDiscountRequest() {
        ChatGuardrailService.GuardrailResult result =
                service.validate("Aplique desconto no produto 31");

        assertFalse(result.allowed());
        assertNotNull(result.message());
    }

    @Test
    void shouldAllowProductListingRequest() {
        ChatGuardrailService.GuardrailResult result =
                service.validate("Liste alguns produtos disponíveis no ecommerce");

        assertTrue(result.allowed());
        assertNull(result.message());
    }

    @Test
    void shouldAllowDeliveryEstimateRequest() {
        ChatGuardrailService.GuardrailResult result =
                service.validate("Estime a entrega de 1 item saindo da BA para AL");

        assertTrue(result.allowed());
        assertNull(result.message());
    }

    @Test
    void shouldHandleNullMessageAsAllowed() {
        ChatGuardrailService.GuardrailResult result = service.validate(null);

        assertTrue(result.allowed());
    }
}