package ai.shreds.domain.events;

import ai.shreds.shared.enums.SharedPaymentStatusEnum;
import java.time.LocalDateTime;

public class PaymentStatusChangedEvent implements DomainEvent {
    private final String paymentId;
    private final SharedPaymentStatusEnum oldStatus;
    private final SharedPaymentStatusEnum newStatus;
    private final LocalDateTime occurredOn;

    public PaymentStatusChangedEvent(String paymentId, SharedPaymentStatusEnum oldStatus, SharedPaymentStatusEnum newStatus) {
        this.paymentId = paymentId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.occurredOn = LocalDateTime.now();
    }

    @Override
    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }

    @Override
    public String getEventType() {
        return "PAYMENT_STATUS_CHANGED";
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