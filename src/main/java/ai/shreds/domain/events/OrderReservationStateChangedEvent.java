package ai.shreds.domain.events;

import ai.shreds.shared.enums.SharedOrderStatusEnum;
import java.time.LocalDateTime;

public class OrderReservationStateChangedEvent implements DomainEvent {
    private final Long orderId;
    private final SharedOrderStatusEnum newState;
    private final LocalDateTime occurredOn;

    public OrderReservationStateChangedEvent(Long orderId, SharedOrderStatusEnum newState) {
        this.orderId = orderId;
        this.newState = newState;
        this.occurredOn = LocalDateTime.now();
    }

    @Override
    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }

    @Override
    public String getEventType() {
        return "ORDER_RESERVATION_STATE_CHANGED";
    }

    public Long getOrderId() {
        return orderId;
    }

    public SharedOrderStatusEnum getNewState() {
        return newState;
    }
}