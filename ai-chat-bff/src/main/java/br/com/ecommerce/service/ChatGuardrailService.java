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
            "cancele",
            "criar",
            "crie",
            "fazer",
            "faca",
            "fechar",
            "feche"
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
            "estoque",
            "pedido",
            "compra"
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
            "de graça"
    );

    public GuardrailResult validate(String message) {
        String normalized = normalize(message);

        if (isDirectlyBlocked(normalized) || isForbiddenActionOnProtectedTarget(normalized)) {
            return GuardrailResult.blocked("""
                    Não posso executar essa solicitação.

                    Nesta versão, eu posso consultar produtos, consultar pedidos e estimar prazos de entrega,
                    mas não posso alterar preços, aplicar descontos, mudar prazos, alterar estoque,
                    cancelar pedidos ou criar compras automaticamente.
                    """);
        }

        return GuardrailResult.permitted();
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