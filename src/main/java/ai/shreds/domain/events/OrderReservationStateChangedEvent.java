package ai.shreds.domain.events;

import ai.shreds.shared.enums.SharedOrderStatusEnum;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Domain event representing a change in order reservation state.
 */
public class OrderReservationStateChangedEvent extends DomainEvent {

    private final SharedOrderStatusEnum oldState;
    private final SharedOrderStatusEnum newState;

    @JsonCreator
    public OrderReservationStateChangedEvent(
            @JsonProperty("orderId") String orderId,
            @JsonProperty("oldState") SharedOrderStatusEnum oldState,
            @JsonProperty("newState") SharedOrderStatusEnum newState) {
        super("ORDER_RESERVATION_STATE_CHANGED", orderId, "ORDER");
        this.oldState = oldState;
        this.newState = newState;
    }

    public SharedOrderStatusEnum getOldState() {
        return oldState;
    }

    public SharedOrderStatusEnum getNewState() {
        return newState;
    }

    @Override
    public String getEventData() {
        return String.format("{\"%s\": \"%s\", \"%s\": \"%s\"}",
            "oldState", oldState,
            "newState", newState);
    }

    @Override
    public String toString() {
        return String.format("%s[orderId=%s, oldState=%s, newState=%s]",
            getClass().getSimpleName(), getAggregateId(), oldState, newState);
    }
}
