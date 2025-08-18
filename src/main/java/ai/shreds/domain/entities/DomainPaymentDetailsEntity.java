package ai.shreds.domain.entities;

import ai.shreds.domain.events.DomainEvent;
import ai.shreds.domain.events.OrderPaymentStatusChangedEvent;
import ai.shreds.domain.exceptions.DomainPaymentException;
import ai.shreds.domain.value_objects.DomainValueMoney;
import ai.shreds.domain.value_objects.DomainValuePaymentStatus;
import ai.shreds.shared.enums.SharedPaymentStatusEnum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Domain entity representing payment details for an order.
 * Maintains payment state and transaction information.
 */
public class DomainPaymentDetailsEntity {

    private static final List<String> VALID_PAYMENT_METHODS = List.of(
        "CREDIT_CARD", "DEBIT_CARD", "BANK_TRANSFER", "DIGITAL_WALLET"
    );

    private String paymentId;
    private Long orderId;
    private String paymentMethod;
    private DomainValueMoney amount;
    private DomainValuePaymentStatus status;
    private List<DomainEvent> domainEvents;

    protected DomainPaymentDetailsEntity() {
        this.domainEvents = new ArrayList<>();
        this.status = DomainValuePaymentStatus.of(SharedPaymentStatusEnum.PENDING);
    }

    public DomainPaymentDetailsEntity(Long orderId, String paymentMethod, DomainValueMoney amount) {
        this();
        validateOrderId(orderId);
        validatePaymentMethod(paymentMethod);
        validateAmount(amount);

        this.orderId = orderId;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
    }

    public void updateStatus(SharedPaymentStatusEnum newStatus) {
        DomainValuePaymentStatus oldStatus = this.status;
        this.status = this.status.transition(newStatus);
        addDomainEvent(new OrderPaymentStatusChangedEvent(
            this.orderId,
            oldStatus.getStatus(),
            newStatus
        ));
    }

    private void validateOrderId(Long orderId) {
        if (orderId == null) {
            throw new DomainPaymentException("Order ID cannot be null");
        }
    }

    private void validatePaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
            throw new DomainPaymentException("Payment method cannot be null or empty");
        }
        if (!VALID_PAYMENT_METHODS.contains(paymentMethod)) {
            throw new DomainPaymentException("Invalid payment method: " + paymentMethod);
        }
    }

    private void validateAmount(DomainValueMoney amount) {
        if (amount == null || amount.isZero()) {
            throw new DomainPaymentException("Payment amount must be positive");
        }
    }

    private void validatePaymentId(String paymentId) {
        if (paymentId == null || paymentId.trim().isEmpty()) {
            throw new DomainPaymentException("Payment ID cannot be null or empty");
        }
    }

    public void validate() {
        validateOrderId(this.orderId);
        validatePaymentMethod(this.paymentMethod);
        validateAmount(this.amount);
        if (this.paymentId != null) {
            validatePaymentId(this.paymentId);
        }
    }

    private void addDomainEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        this.domainEvents.clear();
    }

    // Getters and Setters
    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        validatePaymentId(paymentId);
        this.paymentId = paymentId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public DomainValueMoney getAmount() {
        return amount;
    }

    public DomainValuePaymentStatus getStatus() {
        return status;
    }

    public boolean isPending() {
        return status.isPending();
    }

    public boolean isSuccess() {
        return status.isSuccess();
    }

    public boolean isFailed() {
        return status.isFailed();
    }

    public boolean isDeclined() {
        return status.isDeclined();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainPaymentDetailsEntity that = (DomainPaymentDetailsEntity) o;
        return Objects.equals(paymentId, that.paymentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paymentId);
    }

    @Override
    public String toString() {
        return String.format("PaymentDetails[id=%s, orderId=%d, method=%s, amount=%s, status=%s]",
            paymentId, orderId, paymentMethod, amount, status);
    }
}