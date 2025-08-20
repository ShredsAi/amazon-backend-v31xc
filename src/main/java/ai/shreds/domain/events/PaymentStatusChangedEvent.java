package ai.shreds.domain.events;

import ai.shreds.shared.enums.SharedPaymentStatusEnum;
import java.time.LocalDateTime;

public class PaymentStatusChangedEvent extends DomainEvent {
    private final String paymentId;
    private final SharedPaymentStatusEnum oldStatus;
    private final SharedPaymentStatusEnum newStatus;

    public PaymentStatusChangedEvent(String paymentId, SharedPaymentStatusEnum oldStatus, SharedPaymentStatusEnum newStatus) {
        super();
        this.paymentId = paymentId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public SharedPaymentStatusEnum getOldStatus() {
        return oldStatus;
    }

    public SharedPaymentStatusEnum getNewStatus() {
        return newStatus;
    }
}