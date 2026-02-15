package opgg.ghrami.model;

/**
 * Enum representing the payment status of a booking
 */
public enum PaymentStatus {
    PENDING("pending"),
    PAID("paid"),
    REFUNDED("refunded");
    
    private final String value;
    
    PaymentStatus(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    public static PaymentStatus fromString(String value) {
        for (PaymentStatus status : PaymentStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown payment status: " + value);
    }
}
