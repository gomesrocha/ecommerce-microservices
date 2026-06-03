package br.com.ecommerce.service;

import jakarta.enterprise.context.ApplicationScoped;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class ChatIntentService {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\b(\\d+)\\b");

    private static final Set<String> BRAZIL_STATES = Set.of(
            "AC", "AL", "AM", "AP", "BA", "CE", "DF", "ES", "GO",
            "MA", "MG", "MS", "MT", "PA", "PB", "PE", "PI", "PR",
            "RJ", "RN", "RO", "RR", "RS", "SC", "SE", "SP", "TO"
    );

    public ChatIntent detectIntent(String message) {
        String text = normalize(message);

        if (isCreateOrderIntent(text)) {
            return ChatIntent.CREATE_ORDER;
        }

        if (containsAny(text, "listar produtos", "quais produtos", "produtos disponiveis", "ver produtos")) {
            return ChatIntent.LIST_PRODUCTS;
        }

        if (containsAny(text, "produto", "detalhe do produto", "buscar produto") && extractFirstNumber(text).isPresent()) {
            return ChatIntent.GET_PRODUCT;
        }

        if (containsAny(text, "entrega", "prazo", "estimar entrega", "estime a entrega")) {
            return ChatIntent.ESTIMATE_DELIVERY;
        }

        if (containsAny(text, "pedido", "status do pedido", "consultar pedido") && extractFirstNumber(text).isPresent()) {
            return ChatIntent.GET_ORDER;
        }

        return ChatIntent.GENERAL_CHAT;
    }

    public Optional<Long> extractFirstNumber(String message) {
        Matcher matcher = NUMBER_PATTERN.matcher(message);

        if (matcher.find()) {
            return Optional.of(Long.parseLong(matcher.group(1)));
        }

        return Optional.empty();
    }

    public Optional<RouteInfo> extractRoute(String message) {
        String upper = removeAccents(message).toUpperCase(Locale.ROOT);

        List<String> states = extractBrazilStatesInOrder(upper);

        if (states.size() >= 2) {
            return Optional.of(new RouteInfo(
                    states.get(0),
                    states.get(1),
                    extractTotalItems(message)
            ));
        }

        return Optional.empty();
    }

    public Optional<OrderDraft> extractOrderDraft(String message) {
        Optional<Long> productId = extractProductId(message);
        Optional<String> customerState = extractCustomerState(message);

        if (productId.isEmpty() || customerState.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new OrderDraft(
                productId.get(),
                extractOrderQuantity(message),
                customerState.get(),
                hasExplicitConfirmation(message)
        ));
    }

    private boolean isCreateOrderIntent(String normalized) {
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

    public boolean hasExplicitConfirmation(String message) {
        String normalized = normalize(message);

        return containsAny(
                normalized,
                "confirmo criar pedido",
                "confirmo o pedido",
                "confirmo a compra",
                "confirmar pedido",
                "pode criar o pedido",
                "pode fechar o pedido",
                "sim, criar pedido",
                "sim criar pedido",
                "fechar pedido",
                "finalizar pedido"
        );
    }

    private Optional<Long> extractProductId(String message) {
        String normalized = normalize(message);

        List<Pattern> patterns = List.of(
                Pattern.compile("\\bproduto\\s+(\\d+)\\b"),
                Pattern.compile("\\bproduto\\s+id\\s+(\\d+)\\b"),
                Pattern.compile("\\bid\\s+do\\s+produto\\s+(\\d+)\\b"),
                Pattern.compile("\\bproduct\\s+(\\d+)\\b")
        );

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(normalized);

            if (matcher.find()) {
                return Optional.of(Long.parseLong(matcher.group(1)));
            }
        }

        return extractFirstNumber(message);
    }

    private Integer extractOrderQuantity(String message) {
        String normalized = normalize(message);

        List<Pattern> patterns = List.of(
                Pattern.compile("\\b(\\d+)\\s+(item|itens|unidade|unidades)\\b"),
                Pattern.compile("\\bquantidade\\s+(\\d+)\\b"),
                Pattern.compile("\\bqtd\\s+(\\d+)\\b")
        );

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(normalized);

            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        }

        return 1;
    }

    private Optional<String> extractCustomerState(String message) {
        String upper = removeAccents(message).toUpperCase(Locale.ROOT);

        List<Pattern> patterns = List.of(
                Pattern.compile("\\bPARA\\s+([A-Z]{2})\\b"),
                Pattern.compile("\\bDESTINO\\s+([A-Z]{2})\\b"),
                Pattern.compile("\\bCLIENTE\\s+([A-Z]{2})\\b"),
                Pattern.compile("\\bESTADO\\s+([A-Z]{2})\\b")
        );

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(upper);

            if (matcher.find() && BRAZIL_STATES.contains(matcher.group(1))) {
                return Optional.of(matcher.group(1));
            }
        }

        List<String> states = extractBrazilStatesInOrder(upper);

        if (!states.isEmpty()) {
            return Optional.of(states.get(states.size() - 1));
        }

        return Optional.empty();
    }

    private List<String> extractBrazilStatesInOrder(String upperMessage) {
        List<String> states = new ArrayList<>();

        Matcher matcher = Pattern.compile("\\b[A-Z]{2}\\b").matcher(upperMessage);

        while (matcher.find()) {
            String token = matcher.group();

            if (BRAZIL_STATES.contains(token)) {
                states.add(token);
            }
        }

        return states;
    }

    private Integer extractTotalItems(String message) {
        String normalized = normalize(message);

        Matcher itemMatcher = Pattern.compile("(\\d+)\\s+(item|itens|produto|produtos)").matcher(normalized);

        if (itemMatcher.find()) {
            return Integer.parseInt(itemMatcher.group(1));
        }

        return extractFirstNumber(message)
                .map(Long::intValue)
                .orElse(1);
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
        return removeAccents(value)
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private String removeAccents(String value) {
        if (value == null) {
            return "";
        }

        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }

    public record RouteInfo(
            String originState,
            String destinationState,
            Integer totalItems
    ) {
    }

    public record OrderDraft(
            Long productId,
            Integer quantity,
            String customerState,
            boolean confirmed
    ) {
    }
}
