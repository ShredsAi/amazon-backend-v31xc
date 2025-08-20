package ai.shreds.shared.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Enumeration of possible payment statuses")
public enum SharedPaymentStatusEnum {
    PENDING("Payment is pending processing"),
    SUCCESS("Payment has been successfully processed"),
    FAILED("Payment processing has failed"),
    DECLINED("Payment was declined by the payment provider");

    private final String description;

    SharedPaymentStatusEnum(String description) {
        this.description = description;
    }

    @JsonValue
    public String getValue() {
        return this.name();
    }

    public String getDescription() {
        return description;
    }

    public static SharedPaymentStatusEnum fromString(String status) {
        try {
            return SharedPaymentStatusEnum.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown payment status: " + status);
        }
    }

    public boolean isTerminal() {
        return this == SUCCESS || this == FAILED || this == DECLINED;
    }

    public boolean isSuccessful() {
        return this == SUCCESS;
    }

    public boolean canRetry() {
        return this == FAILED || this == DECLINED;
    }

    public boolean canTransitionTo(SharedPaymentStatusEnum newStatus) {
        if (this.isTerminal()) {
            return false;
        }

        return this == PENDING; // PENDING can transition to any other status
    }
}
