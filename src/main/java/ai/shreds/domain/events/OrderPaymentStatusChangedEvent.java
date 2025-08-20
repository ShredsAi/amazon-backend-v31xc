package ai.shreds.domain.events;

import ai.shreds.domain.value_objects.DomainValuePaymentStatus;
import ai.shreds.shared.enums.SharedPaymentStatusEnum;

public class OrderPaymentStatusChangedEvent extends DomainEvent {
    private final Long orderId;
    private final SharedPaymentStatusEnum oldStatus;
    private final SharedPaymentStatusEnum newStatus;

    public OrderPaymentStatusChangedEvent(Long orderId, SharedPaymentStatusEnum oldStatus, SharedPaymentStatusEnum newStatus) {
        super();
        this.orderId = orderId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    public Long getOrderId() {
        return orderId;
    }

    public SharedPaymentStatusEnum getOldStatus() {
        return oldStatus;
    }

    public SharedPaymentStatusEnum getNewStatus() {
        return newStatus;
    }
}