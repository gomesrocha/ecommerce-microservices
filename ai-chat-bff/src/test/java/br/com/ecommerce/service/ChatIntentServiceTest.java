package br.com.ecommerce.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatIntentServiceTest {

    private final ChatIntentService service = new ChatIntentService();

    @Test
    void shouldDetectListProductsIntent() {
        ChatIntent intent = service.detectIntent("Liste alguns produtos disponíveis no ecommerce");

        assertEquals(ChatIntent.LIST_PRODUCTS, intent);
    }

    @Test
    void shouldDetectGetProductIntent() {
        ChatIntent intent = service.detectIntent("Consultar produto 31");

        assertEquals(ChatIntent.GET_PRODUCT, intent);
    }

    @Test
    void shouldDetectEstimateDeliveryIntent() {
        ChatIntent intent = service.detectIntent("Estime a entrega de 1 item saindo da BA para AL");

        assertEquals(ChatIntent.ESTIMATE_DELIVERY, intent);
    }

    @Test
    void shouldDetectGetOrderIntent() {
        ChatIntent intent = service.detectIntent("Consulte o pedido 10");

        assertEquals(ChatIntent.GET_ORDER, intent);
    }

    @Test
    void shouldExtractRouteIgnoringPrepositionDe() {
        ChatIntentService.RouteInfo route = service
                .extractRoute("Estime a entrega de 1 item saindo da BA para AL")
                .orElseThrow();

        assertEquals("BA", route.originState());
        assertEquals("AL", route.destinationState());
        assertEquals(1, route.totalItems());
    }

    @Test
    void shouldExtractRouteAndTotalItems() {
        ChatIntentService.RouteInfo route = service
                .extractRoute("Qual o prazo de 3 itens de SP para SE?")
                .orElseThrow();

        assertEquals("SP", route.originState());
        assertEquals("SE", route.destinationState());
        assertEquals(3, route.totalItems());
    }

    @Test
    void shouldReturnEmptyRouteWhenStatesAreMissing() {
        assertTrue(service.extractRoute("Qual o prazo de entrega?").isEmpty());
    }

    @Test
    void shouldExtractFirstNumber() {
        Long number = service.extractFirstNumber("Consultar pedido 123")
                .orElseThrow();

        assertEquals(123L, number);
    }
}