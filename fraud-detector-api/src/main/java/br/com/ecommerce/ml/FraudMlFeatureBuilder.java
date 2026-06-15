package br.com.ecommerce.ml;

import org.tribuo.Feature;
import org.tribuo.classification.Label;
import org.tribuo.impl.ArrayExample;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class FraudMlFeatureBuilder {

    private FraudMlFeatureBuilder() {
    }

    public static ArrayExample<Label> buildExample(
            BigDecimal totalAmount,
            Integer itemsQuantity,
            BigDecimal avgItemPrice,
            BigDecimal maxItemPrice,
            Integer uniqueProducts,
            LocalDateTime occurredAt
    ) {
        double totalAmountValue = toDouble(totalAmount);
        double itemsQuantityValue = normalizeItemsQuantity(itemsQuantity);
        double avgItemPriceValue = toDouble(avgItemPrice);
        double maxItemPriceValue = toDouble(maxItemPrice);
        double uniqueProductsValue = normalizeUniqueProducts(uniqueProducts);

        LocalDateTime transactionTime = occurredAt == null
                ? LocalDateTime.now()
                : occurredAt;

        double amountPerItem = safeDivide(totalAmountValue, itemsQuantityValue);
        double priceSpread = maxItemPriceValue - avgItemPriceValue;
        double maxToAvgPriceRatio = safeDivide(maxItemPriceValue, avgItemPriceValue);

        int transactionHour = transactionTime.getHour();
        int transactionDayOfWeek = transactionTime.getDayOfWeek().getValue() - 1;
        boolean weekend = isWeekend(transactionTime.getDayOfWeek());

        String amountBucket = amountBucket(totalAmountValue);
        String itemsBucket = itemsBucket(itemsQuantityValue);
        String hourBucket = hourBucket(transactionHour);

        List<Feature> features = new ArrayList<>();

        features.add(new Feature("total_amount", totalAmountValue));
        features.add(new Feature("log_total_amount", Math.log1p(totalAmountValue)));

        features.add(new Feature("items_quantity", itemsQuantityValue));
        features.add(new Feature("log_items_quantity", Math.log1p(itemsQuantityValue)));

        features.add(new Feature("avg_item_price", avgItemPriceValue));
        features.add(new Feature("log_avg_item_price", Math.log1p(avgItemPriceValue)));

        features.add(new Feature("max_item_price", maxItemPriceValue));
        features.add(new Feature("log_max_item_price", Math.log1p(maxItemPriceValue)));

        features.add(new Feature("unique_products", uniqueProductsValue));
        features.add(new Feature("amount_per_item", amountPerItem));
        features.add(new Feature("price_spread", priceSpread));
        features.add(new Feature("max_to_avg_price_ratio", maxToAvgPriceRatio));

        features.add(new Feature("transaction_hour", transactionHour));
        features.add(new Feature("transaction_day_of_week", transactionDayOfWeek));
        features.add(new Feature("transaction_is_weekend", weekend ? 1.0 : 0.0));

        features.add(new Feature("amount_low", "low".equals(amountBucket) ? 1.0 : 0.0));
        features.add(new Feature("amount_medium", "medium".equals(amountBucket) ? 1.0 : 0.0));
        features.add(new Feature("amount_high", "high".equals(amountBucket) ? 1.0 : 0.0));
        features.add(new Feature("amount_very_high", "very_high".equals(amountBucket) ? 1.0 : 0.0));

        features.add(new Feature("items_single", "single".equals(itemsBucket) ? 1.0 : 0.0));
        features.add(new Feature("items_few", "few".equals(itemsBucket) ? 1.0 : 0.0));
        features.add(new Feature("items_many", "many".equals(itemsBucket) ? 1.0 : 0.0));

        features.add(new Feature("hour_night", "night".equals(hourBucket) ? 1.0 : 0.0));
        features.add(new Feature("hour_morning", "morning".equals(hourBucket) ? 1.0 : 0.0));
        features.add(new Feature("hour_afternoon", "afternoon".equals(hourBucket) ? 1.0 : 0.0));
        features.add(new Feature("hour_evening", "evening".equals(hourBucket) ? 1.0 : 0.0));

        return new ArrayExample<>(
                new Label("UNKNOWN"),
                features
        );
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
        return buildExample(
                totalAmount,
                itemsQuantity,
                avgItemPrice,
                maxItemPrice,
                uniqueProducts,
                LocalDateTime.now()
        );
    }

    private static double toDouble(BigDecimal value) {
        if (value == null) {
            return 0.0;
        }

        return value.doubleValue();
    }

    private static double normalizeItemsQuantity(Integer value) {
        if (value == null || value <= 0) {
            return 1.0;
        }

        return value.doubleValue();
    }

    private static double normalizeUniqueProducts(Integer value) {
        if (value == null || value <= 0) {
            return 1.0;
        }

        return value.doubleValue();
    }

    private static double safeDivide(double numerator, double denominator) {
        if (denominator == 0.0) {
            return 0.0;
        }

        return numerator / denominator;
    }

    private static String amountBucket(double totalAmount) {
        if (totalAmount <= 50) {
            return "low";
        }

        if (totalAmount <= 200) {
            return "medium";
        }

        if (totalAmount <= 500) {
            return "high";
        }

        return "very_high";
    }

    private static String itemsBucket(double itemsQuantity) {
        if (itemsQuantity <= 1) {
            return "single";
        }

        if (itemsQuantity <= 3) {
            return "few";
        }

        return "many";
    }

    private static String hourBucket(int hour) {
        if (hour >= 0 && hour <= 5) {
            return "night";
        }

        if (hour >= 6 && hour <= 11) {
            return "morning";
        }

        if (hour >= 12 && hour <= 17) {
            return "afternoon";
        }

        return "evening";
    }

    private static boolean isWeekend(DayOfWeek dayOfWeek) {
        return DayOfWeek.SATURDAY.equals(dayOfWeek)
                || DayOfWeek.SUNDAY.equals(dayOfWeek);
    }
}
