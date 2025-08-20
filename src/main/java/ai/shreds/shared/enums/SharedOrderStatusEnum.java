package ai.shreds.shared.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Enumeration of possible order statuses")
public enum SharedOrderStatusEnum {
    PENDING("Order is pending processing"),
    RESERVED("Items have been reserved in inventory"),
    COMPLETED("Order has been successfully completed"),
    CANCELLED("Order has been cancelled");

    private final String description;

    SharedOrderStatusEnum(String description) {
        this.description = description;
    }

    @JsonValue
    public String getValue() {
        return this.name();
    }

    public String getDescription() {
        return description;
    }

    public static SharedOrderStatusEnum fromString(String status) {
        try {
            return SharedOrderStatusEnum.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown order status: " + status);
        }
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }

    public boolean canTransitionTo(SharedOrderStatusEnum newStatus) {
        if (this.isTerminal()) {
            return false;
        }

        switch (this) {
            case PENDING:
                return newStatus == RESERVED || newStatus == CANCELLED;
            case RESERVED:
                return newStatus == COMPLETED || newStatus == CANCELLED;
            default:
                return false;
        }
    }
}
