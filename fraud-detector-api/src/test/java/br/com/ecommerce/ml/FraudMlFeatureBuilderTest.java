package br.com.ecommerce.ml;

import org.junit.jupiter.api.Test;
import org.tribuo.Feature;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class FraudMlFeatureBuilderTest {

    @Test
    void shouldBuildExampleWithNumericAndRouteFeatures() {
        var example = FraudMlFeatureBuilder.buildExample(
                new BigDecimal("1000.00"),
                2,
                new BigDecimal("500.00"),
                new BigDecimal("700.00"),
                2,
                "SP",
                "SE"
        );

        assertEquals(1000.00, featureValue(example, "total_amount"));
        assertEquals(2.0, featureValue(example, "items_quantity"));
        assertEquals(500.00, featureValue(example, "avg_item_price"));
        assertEquals(700.00, featureValue(example, "max_item_price"));
        assertEquals(2.0, featureValue(example, "unique_products"));
        assertEquals(1.0, featureValue(example, "origin_SP"));
        assertEquals(1.0, featureValue(example, "destination_SE"));
    }

    @Test
    void shouldNormalizeStates() {
        var example = FraudMlFeatureBuilder.buildExample(
                new BigDecimal("100.00"),
                1,
                new BigDecimal("100.00"),
                new BigDecimal("100.00"),
                1,
                " ba ",
                " al "
        );

        assertEquals(1.0, featureValue(example, "origin_BA"));
        assertEquals(1.0, featureValue(example, "destination_AL"));
    }

    @Test
    void shouldHandleNullValuesAsZero() {
        var example = FraudMlFeatureBuilder.buildExample(
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertEquals(0.0, featureValue(example, "total_amount"));
        assertEquals(0.0, featureValue(example, "items_quantity"));
        assertEquals(0.0, featureValue(example, "avg_item_price"));
        assertEquals(0.0, featureValue(example, "max_item_price"));
        assertEquals(0.0, featureValue(example, "unique_products"));
    }

    private double featureValue(Iterable<Feature> example, String featureName) {
        for (Feature feature : example) {
            if (feature.getName().equals(featureName)) {
                return feature.getValue();
            }
        }

        fail("Feature não encontrada: " + featureName);
        return 0.0;
    }
}