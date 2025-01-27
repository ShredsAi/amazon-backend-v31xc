package ai.shreds.domain.events;

import ai.shreds.shared.enums.SharedPaymentStatusEnum;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Domain event representing a change in order payment status.
 */
public class OrderPaymentStatusChangedEvent extends DomainEvent {

    private final SharedPaymentStatusEnum oldStatus;
    private final SharedPaymentStatusEnum newStatus;

    @JsonCreator
    public OrderPaymentStatusChangedEvent(
            @JsonProperty("orderId") String orderId,
            @JsonProperty("oldStatus") SharedPaymentStatusEnum oldStatus,
            @JsonProperty("newStatus") SharedPaymentStatusEnum newStatus) {
        super("ORDER_PAYMENT_STATUS_CHANGED", orderId, "ORDER");
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    public SharedPaymentStatusEnum getOldStatus() {
        return oldStatus;
    }

    public SharedPaymentStatusEnum getNewStatus() {
        return newStatus;
    }

    @Override
    public String getEventData() {
        return String.format("{\"%s\": \"%s\", \"%s\": \"%s\"}",
            "oldStatus", oldStatus,
            "newStatus", newStatus);
    }

    @Override
    public String toString() {
        return String.format("%s[orderId=%s, oldStatus=%s, newStatus=%s]",
            getClass().getSimpleName(), getAggregateId(), oldStatus, newStatus);
    }
}
