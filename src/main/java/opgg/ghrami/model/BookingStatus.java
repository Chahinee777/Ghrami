package opgg.ghrami.model;

/**
 * Enum representing the status of a booking
 */
public enum BookingStatus {
    SCHEDULED("scheduled"),
    COMPLETED("completed"),
    CANCELLED("cancelled");
    
    private final String value;
    
    BookingStatus(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    public static BookingStatus fromString(String value) {
        for (BookingStatus status : BookingStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown booking status: " + value);
    }
}
