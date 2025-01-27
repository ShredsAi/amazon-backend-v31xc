package ai.shreds.domain.entities;

import ai.shreds.domain.exceptions.DomainPaymentException;
import ai.shreds.domain.value_objects.DomainValueMoney;
import ai.shreds.domain.value_objects.DomainValuePaymentStatus;
import ai.shreds.shared.enums.SharedPaymentStatusEnum;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class DomainEntityPaymentDetails {

    private static final List<String> VALID_PAYMENT_METHODS = List.of(
        "CREDIT_CARD", "DEBIT_CARD", "BANK_TRANSFER", "DIGITAL_WALLET"
    );

    private String paymentId;
    private Long orderId;
    private String paymentMethod;
    private DomainValueMoney amount;
    private DomainValuePaymentStatus status;
    private List<DomainEvent> domainEvents;

    public DomainEntityPaymentDetails() {
        this.domainEvents = new ArrayList<>();
        this.status = new DomainValuePaymentStatus(SharedPaymentStatusEnum.PENDING);
    }

    public DomainEntityPaymentDetails(Long orderId, String paymentMethod, DomainValueMoney amount) {
        this();
        validateOrderId(orderId);
        validatePaymentMethod(paymentMethod);
        validateAmount(amount);

        this.orderId = orderId;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
    }

    public void updateStatus(SharedPaymentStatusEnum newStatus) {
        validateStatusTransition(newStatus);
        DomainValuePaymentStatus oldStatus = this.status;
        this.status = new DomainValuePaymentStatus(newStatus);
        addDomainEvent(new PaymentStatusChangedEvent(this.paymentId, oldStatus.getStatus(), newStatus));
    }

    private void validateStatusTransition(SharedPaymentStatusEnum newStatus) {
        if (status.getStatus() != SharedPaymentStatusEnum.PENDING) {
            throw new DomainPaymentException("Cannot change status of non-pending payment");
        }
        if (!List.of(SharedPaymentStatusEnum.SUCCESS, SharedPaymentStatusEnum.FAILED, 
                     SharedPaymentStatusEnum.DECLINED).contains(newStatus)) {
            throw new DomainPaymentException("Invalid payment status transition");
        }
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
        if (amount == null || amount.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainPaymentException("Payment amount must be positive");
        }
    }

    public void validate() {
        validateOrderId(this.orderId);
        validatePaymentMethod(this.paymentMethod);
        validateAmount(this.amount);
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

    // Getters and Setters with validation
    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        if (paymentId == null || paymentId.trim().isEmpty()) {
            throw new DomainPaymentException("Payment ID cannot be null or empty");
        }
        this.paymentId = paymentId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        validateOrderId(orderId);
        this.orderId = orderId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        validatePaymentMethod(paymentMethod);
        this.paymentMethod = paymentMethod;
    }

    public DomainValueMoney getAmount() {
        return amount;
    }

    public void setAmount(DomainValueMoney amount) {
        validateAmount(amount);
        this.amount = amount;
    }

    public DomainValuePaymentStatus getStatus() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainEntityPaymentDetails that = (DomainEntityPaymentDetails) o;
        return Objects.equals(paymentId, that.paymentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paymentId);
    }

    @Override
    public String toString() {
        return "DomainEntityPaymentDetails{" +
                "paymentId='" + paymentId + '\'' +
                ", orderId=" + orderId +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", amount=" + amount +
                ", status=" + status +
                '}';
    }
}
