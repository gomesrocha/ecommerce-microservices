package br.com.ecommerce.service;

import jakarta.enterprise.context.ApplicationScoped;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class ChatGuardrailService {

    private static final List<String> FORBIDDEN_ACTIONS = List.of(
            "mudar",
            "mude",
            "alterar",
            "altere",
            "trocar",
            "troque",
            "reduzir",
            "reduza",
            "baixar",
            "baixe",
            "diminuir",
            "diminua",
            "dar",
            "aplicar",
            "aplique",
            "colocar",
            "coloque",
            "definir",
            "defina",
            "forcar",
            "force",
            "ignorar",
            "ignore",
            "burlar",
            "cancelar",
            "cancele"
    );

    private static final List<String> PROTECTED_TARGETS = List.of(
            "preco",
            "valor",
            "custo",
            "desconto",
            "cupom",
            "frete",
            "prazo",
            "entrega",
            "estoque"
    );

    private static final List<String> DIRECT_BLOCKED_PATTERNS = List.of(
            "frete gratis",
            "entrega amanha",
            "entregar amanha",
            "preco menor",
            "valor menor",
            "mais barato",
            "ignorar regra",
            "ignore as regras",
            "sem pagar",
            "de graca",
            "de graça",
            "com desconto",
            "aplicar desconto",
            "dar desconto",
            "cupom gratis",
            "cupom gratuito"
    );

    public GuardrailResult validate(String message) {
        String normalized = normalize(message);

        if (isOrderCreationRequest(normalized)) {
            if (isDirectlyBlocked(normalized) || containsUnsafeCommercialManipulation(normalized)) {
                return GuardrailResult.blocked("""
                        Não posso criar pedido com alteração de preço, desconto, frete grátis,
                        manipulação de prazo, alteração de estoque ou tentativa de ignorar regras de negócio.

                        Posso criar um pedido apenas com produto, quantidade e estado de entrega válidos,
                        e somente após confirmação explícita.
                        """);
            }

            return GuardrailResult.permitted();
        }

        if (isDirectlyBlocked(normalized) || isForbiddenActionOnProtectedTarget(normalized)) {
            return GuardrailResult.blocked("""
                    Não posso executar essa solicitação.

                    Nesta versão, eu posso consultar produtos, consultar pedidos, estimar prazos de entrega
                    e criar pedidos somente com confirmação explícita.
                    Não posso alterar preços, aplicar descontos, mudar prazos, alterar estoque,
                    cancelar pedidos ou executar ações administrativas.
                    """);
        }

        return GuardrailResult.permitted();
    }

    private boolean isOrderCreationRequest(String normalized) {
        return containsAny(
                normalized,
                "criar pedido",
                "fazer pedido",
                "fechar pedido",
                "finalizar pedido",
                "comprar produto",
                "quero comprar",
                "confirmo criar pedido",
                "confirmar pedido",
                "confirmo o pedido"
        );
    }

    private boolean containsUnsafeCommercialManipulation(String normalized) {
        return containsAny(
                normalized,
                "desconto",
                "cupom",
                "frete gratis",
                "frete gratuito",
                "sem pagar",
                "preco menor",
                "valor menor",
                "mais barato",
                "entrega amanha",
                "prazo menor",
                "alterar estoque",
                "mudar estoque"
        );
    }

    private boolean isDirectlyBlocked(String normalized) {
        return DIRECT_BLOCKED_PATTERNS.stream()
                .anyMatch(normalized::contains);
    }

    private boolean isForbiddenActionOnProtectedTarget(String normalized) {
        boolean hasForbiddenAction = FORBIDDEN_ACTIONS.stream()
                .anyMatch(action -> containsWord(normalized, action));

        boolean hasProtectedTarget = PROTECTED_TARGETS.stream()
                .anyMatch(target -> containsWord(normalized, target));

        return hasForbiddenAction && hasProtectedTarget;
    }

    private boolean containsWord(String text, String word) {
        return text.matches(".*\\b" + word + "\\b.*");
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value)) {
                return true;
            }
        }

        return false;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return withoutAccents
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    public record GuardrailResult(
            boolean allowed,
            String message
    ) {
        public static GuardrailResult permitted() {
            return new GuardrailResult(true, null);
        }

        public static GuardrailResult blocked(String message) {
            return new GuardrailResult(false, message);
        }
    }
}
