package ai.shreds.domain.events;

import ai.shreds.domain.value_objects.DomainValueOrderStatus;
import ai.shreds.shared.enums.SharedOrderStatusEnum;

public class OrderReservationStateChangedEvent extends DomainEvent {
    private final Long orderId;
    private final SharedOrderStatusEnum oldState;
    private final SharedOrderStatusEnum newState;

    public OrderReservationStateChangedEvent(Long orderId, SharedOrderStatusEnum oldState, SharedOrderStatusEnum newState) {
        super();
        this.orderId = orderId;
        this.oldState = oldState;
        this.newState = newState;
    }

    public Long getOrderId() {
        return orderId;
    }

    public SharedOrderStatusEnum getOldState() {
        return oldState;
    }

    public SharedOrderStatusEnum getNewState() {
        return newState;
    }
}