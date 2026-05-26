package br.com.ecommerce.ml;

import org.tribuo.Feature;
import org.tribuo.classification.Label;
import org.tribuo.impl.ArrayExample;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class FraudMlFeatureBuilder {

    private static final List<String> BRAZIL_STATES = List.of(
            "AC", "AL", "AM", "AP", "BA", "CE", "DF", "ES", "GO",
            "MA", "MG", "MS", "MT", "PA", "PB", "PE", "PI", "PR",
            "RJ", "RN", "RO", "RR", "RS", "SC", "SE", "SP", "TO"
    );

    private FraudMlFeatureBuilder() {
    }

    public static ArrayExample<Label> buildExample(
            BigDecimal totalAmount,
            Integer itemsQuantity,
            BigDecimal avgItemPrice,
            BigDecimal maxItemPrice,
            Integer uniqueProducts,
            String originState,
            String destinationState
    ) {
        String origin = normalizeState(originState);
        String destination = normalizeState(destinationState);

        List<Feature> features = new ArrayList<>();

        features.add(new Feature("total_amount", toDouble(totalAmount)));
        features.add(new Feature("items_quantity", toDouble(itemsQuantity)));
        features.add(new Feature("avg_item_price", toDouble(avgItemPrice)));
        features.add(new Feature("max_item_price", toDouble(maxItemPrice)));
        features.add(new Feature("unique_products", toDouble(uniqueProducts)));

        for (String state : BRAZIL_STATES) {
            features.add(new Feature("origin_" + state, state.equals(origin) ? 1.0 : 0.0));
        }

        for (String state : BRAZIL_STATES) {
            features.add(new Feature("destination_" + state, state.equals(destination) ? 1.0 : 0.0));
        }

        return new ArrayExample<>(
                new Label("UNKNOWN"),
                features
        );
    }

    private static String normalizeState(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static double toDouble(BigDecimal value) {
        if (value == null) {
            return 0.0;
        }

        return value.doubleValue();
    }

    private static double toDouble(Integer value) {
        if (value == null) {
            return 0.0;
        }

        return value.doubleValue();
    }
}