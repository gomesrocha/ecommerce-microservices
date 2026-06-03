package br.com.ecommerce.ml;

import org.tribuo.Feature;
import org.tribuo.impl.ArrayExample;
import org.tribuo.regression.Regressor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DeliveryMlFeatureBuilder {

    private static final List<String> BRAZIL_STATES = List.of(
            "AC", "AL", "AM", "AP", "BA", "CE", "DF", "ES", "GO",
            "MA", "MG", "MS", "MT", "PA", "PB", "PE", "PI", "PR",
            "RJ", "RN", "RO", "RR", "RS", "SC", "SE", "SP", "TO"
    );

    private DeliveryMlFeatureBuilder() {
    }

    public static ArrayExample<Regressor> buildExample(
            String originState,
            String destinationState,
            Integer totalItems
    ) {
        String origin = normalizeState(originState);
        String destination = normalizeState(destinationState);

        double items = totalItems == null || totalItems <= 0 ? 1.0 : totalItems.doubleValue();

        List<Feature> features = new ArrayList<>();

        features.add(new Feature("items_quantity", items));

        for (String state : BRAZIL_STATES) {
            features.add(new Feature("origin_" + state, state.equals(origin) ? 1.0 : 0.0));
        }

        for (String state : BRAZIL_STATES) {
            features.add(new Feature("destination_" + state, state.equals(destination) ? 1.0 : 0.0));
        }

        return new ArrayExample<>(
                new Regressor("delivery_days", Double.NaN),
                features
        );
    }

    private static String normalizeState(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return value.trim().toUpperCase(Locale.ROOT);
    }
}