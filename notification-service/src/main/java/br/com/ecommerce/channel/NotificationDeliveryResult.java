package br.com.ecommerce.channel;

public record NotificationDeliveryResult(
        boolean success,
        String errorMessage
) {
    public static NotificationDeliveryResult sent() {
        return new NotificationDeliveryResult(true, null);
    }

    public static NotificationDeliveryResult failed(String errorMessage) {
        return new NotificationDeliveryResult(false, errorMessage);
    }
}