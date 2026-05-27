package br.com.ecommerce.ml;

import org.junit.jupiter.api.Test;
import org.tribuo.Feature;

import static org.junit.jupiter.api.Assertions.*;

class DeliveryMlFeatureBuilderTest {

    @Test
    void shouldBuildExampleWithRouteAndItemsFeatures() {
        var example = DeliveryMlFeatureBuilder.buildExample(
                "BA",
                "AL",
                3
        );

        assertEquals(3.0, featureValue(example, "items_quantity"));
        assertEquals(1.0, featureValue(example, "origin_BA"));
        assertEquals(0.0, featureValue(example, "origin_SP"));
        assertEquals(1.0, featureValue(example, "destination_AL"));
        assertEquals(0.0, featureValue(example, "destination_SE"));
    }

    @Test
    void shouldNormalizeStates() {
        var example = DeliveryMlFeatureBuilder.buildExample(
                " ba ",
                " al ",
                1
        );

        assertEquals(1.0, featureValue(example, "origin_BA"));
        assertEquals(1.0, featureValue(example, "destination_AL"));
    }

    @Test
    void shouldHandleNullTotalItemsAsOne() {
        var example = DeliveryMlFeatureBuilder.buildExample(
                "SP",
                "SE",
                null
        );

        assertEquals(1.0, featureValue(example, "items_quantity"));
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