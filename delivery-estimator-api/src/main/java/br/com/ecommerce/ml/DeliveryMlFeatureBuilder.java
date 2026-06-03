package br.com.ecommerce.ml;

import org.tribuo.Feature;
import org.tribuo.impl.ArrayExample;
import org.tribuo.regression.Regressor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class DeliveryMlFeatureBuilder {

    private static final String TARGET_NAME = "delivery_days";

    private static final List<String> STATES = List.of(
            "AC", "AL", "AM", "AP", "BA", "CE", "DF", "ES", "GO",
            "MA", "MG", "MS", "MT", "PA", "PB", "PE", "PI", "PR",
            "RJ", "RN", "RO", "RR", "RS", "SC", "SE", "SP", "TO"
    );

    private static final List<String> REGIONS = List.of(
            "NORTE",
            "NORDESTE",
            "CENTRO_OESTE",
            "SUDESTE",
            "SUL"
    );

    private static final List<String> DISTANCE_BUCKETS = List.of(
            "local",
            "short",
            "medium",
            "long",
            "very_long"
    );

    private static final Map<String, String> REGIONS_BY_STATE = Map.ofEntries(
            Map.entry("AC", "NORTE"),
            Map.entry("AP", "NORTE"),
            Map.entry("AM", "NORTE"),
            Map.entry("PA", "NORTE"),
            Map.entry("RO", "NORTE"),
            Map.entry("RR", "NORTE"),
            Map.entry("TO", "NORTE"),

            Map.entry("AL", "NORDESTE"),
            Map.entry("BA", "NORDESTE"),
            Map.entry("CE", "NORDESTE"),
            Map.entry("MA", "NORDESTE"),
            Map.entry("PB", "NORDESTE"),
            Map.entry("PE", "NORDESTE"),
            Map.entry("PI", "NORDESTE"),
            Map.entry("RN", "NORDESTE"),
            Map.entry("SE", "NORDESTE"),

            Map.entry("DF", "CENTRO_OESTE"),
            Map.entry("GO", "CENTRO_OESTE"),
            Map.entry("MT", "CENTRO_OESTE"),
            Map.entry("MS", "CENTRO_OESTE"),

            Map.entry("ES", "SUDESTE"),
            Map.entry("MG", "SUDESTE"),
            Map.entry("RJ", "SUDESTE"),
            Map.entry("SP", "SUDESTE"),

            Map.entry("PR", "SUL"),
            Map.entry("RS", "SUL"),
            Map.entry("SC", "SUL")
    );

    private static final Map<String, double[]> STATE_COORDS = Map.ofEntries(
            Map.entry("AC", new double[]{-9.97, -67.81}),
            Map.entry("AL", new double[]{-9.66, -35.73}),
            Map.entry("AM", new double[]{-3.10, -60.02}),
            Map.entry("AP", new double[]{0.03, -51.05}),
            Map.entry("BA", new double[]{-12.97, -38.50}),
            Map.entry("CE", new double[]{-3.73, -38.52}),
            Map.entry("DF", new double[]{-15.79, -47.88}),
            Map.entry("ES", new double[]{-20.31, -40.31}),
            Map.entry("GO", new double[]{-16.68, -49.25}),
            Map.entry("MA", new double[]{-2.53, -44.30}),
            Map.entry("MG", new double[]{-19.92, -43.94}),
            Map.entry("MS", new double[]{-20.45, -54.62}),
            Map.entry("MT", new double[]{-15.60, -56.10}),
            Map.entry("PA", new double[]{-1.45, -48.50}),
            Map.entry("PB", new double[]{-7.12, -34.86}),
            Map.entry("PE", new double[]{-8.05, -34.90}),
            Map.entry("PI", new double[]{-5.09, -42.80}),
            Map.entry("PR", new double[]{-25.43, -49.27}),
            Map.entry("RJ", new double[]{-22.91, -43.17}),
            Map.entry("RN", new double[]{-5.79, -35.21}),
            Map.entry("RO", new double[]{-8.76, -63.90}),
            Map.entry("RR", new double[]{2.82, -60.67}),
            Map.entry("RS", new double[]{-30.03, -51.23}),
            Map.entry("SC", new double[]{-27.59, -48.55}),
            Map.entry("SE", new double[]{-10.91, -37.07}),
            Map.entry("SP", new double[]{-23.55, -46.63}),
            Map.entry("TO", new double[]{-10.18, -48.33})
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

        String originRegion = REGIONS_BY_STATE.getOrDefault(origin, "SUDESTE");
        String destinationRegion = REGIONS_BY_STATE.getOrDefault(destination, "SUDESTE");

        double distanceKm = haversineKm(origin, destination);
        String distanceBucket = distanceBucket(distanceKm);

        List<Feature> features = new ArrayList<>();

        features.add(new Feature("items_quantity", items));
        features.add(new Feature("log_items_quantity", Math.log1p(items)));
        features.add(new Feature("same_state", origin.equals(destination) ? 1.0 : 0.0));
        features.add(new Feature("same_region", originRegion.equals(destinationRegion) ? 1.0 : 0.0));
        features.add(new Feature("is_interstate", origin.equals(destination) ? 0.0 : 1.0));
        features.add(new Feature("route_distance_km", distanceKm));

        for (String bucket : DISTANCE_BUCKETS) {
            features.add(new Feature(
                    "distance_" + bucket,
                    bucket.equals(distanceBucket) ? 1.0 : 0.0
            ));
        }

        for (String region : REGIONS) {
            features.add(new Feature(
                    "origin_region_" + region,
                    region.equals(originRegion) ? 1.0 : 0.0
            ));
        }

        for (String region : REGIONS) {
            features.add(new Feature(
                    "destination_region_" + region,
                    region.equals(destinationRegion) ? 1.0 : 0.0
            ));
        }

        for (String state : STATES) {
            features.add(new Feature("origin_" + state, state.equals(origin) ? 1.0 : 0.0));
        }

        for (String state : STATES) {
            features.add(new Feature("destination_" + state, state.equals(destination) ? 1.0 : 0.0));
        }

        return new ArrayExample<>(
                new Regressor(TARGET_NAME, Double.NaN),
                features
        );
    }

    private static String normalizeState(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static double haversineKm(String origin, String destination) {
        double[] originCoords = STATE_COORDS.get(origin);
        double[] destinationCoords = STATE_COORDS.get(destination);

        if (originCoords == null || destinationCoords == null) {
            return 0.0;
        }

        double lat1 = originCoords[0];
        double lon1 = originCoords[1];
        double lat2 = destinationCoords[0];
        double lon2 = destinationCoords[1];

        double radiusKm = 6371.0;

        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = Math.toRadians(lat2 - lat1);
        double deltaLambda = Math.toRadians(lon2 - lon1);

        double a = Math.pow(Math.sin(deltaPhi / 2.0), 2)
                + Math.cos(phi1) * Math.cos(phi2)
                * Math.pow(Math.sin(deltaLambda / 2.0), 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return Math.round(radiusKm * c * 100.0) / 100.0;
    }

    private static String distanceBucket(double distanceKm) {
        if (distanceKm <= 150) {
            return "local";
        }

        if (distanceKm <= 600) {
            return "short";
        }

        if (distanceKm <= 1200) {
            return "medium";
        }

        if (distanceKm <= 2200) {
            return "long";
        }

        return "very_long";
    }
}