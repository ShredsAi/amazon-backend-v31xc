package ai.shreds.domain.entities;

import ai.shreds.domain.exceptions.DomainExceptionInvalidOrder;
import ai.shreds.domain.value_objects.DomainValueMoney;
import ai.shreds.domain.value_objects.DomainValueOrderStatus;
import ai.shreds.domain.value_objects.DomainValuePaymentStatus;
import ai.shreds.shared.enums.SharedOrderStatusEnum;
import ai.shreds.shared.enums.SharedPaymentStatusEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class DomainEntityOrder {

    private Long id;
    private Long userId;
    private DomainValueMoney totalAmount;
    private DomainValuePaymentStatus paymentStatus;
    private DomainValueOrderStatus reservationState;
    private LocalDateTime createdAt;
    private List<DomainEntityOrderItem> items;
    private List<DomainEvent> domainEvents;

    public DomainEntityOrder() {
        this.items = new ArrayList<>();
        this.domainEvents = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.paymentStatus = new DomainValuePaymentStatus(SharedPaymentStatusEnum.PENDING);
        this.reservationState = new DomainValueOrderStatus(SharedOrderStatusEnum.PENDING);
        this.totalAmount = new DomainValueMoney(BigDecimal.ZERO, "USD");
    }

    public void addItem(DomainEntityOrderItem item) {
        validateItem(item);
        items.add(item);
        calculateTotalAmount();
    }

    public void removeItem(DomainEntityOrderItem item) {
        items.remove(item);
        calculateTotalAmount();
    }

    private void validateItem(DomainEntityOrderItem item) {
        if (item == null) {
            throw new DomainExceptionInvalidOrder("Order item cannot be null");
        }
        if (item.getQuantity() <= 0) {
            throw new DomainExceptionInvalidOrder("Item quantity must be positive");
        }
        if (item.getPrice() == null || item.getPrice().getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainExceptionInvalidOrder("Item price must be positive");
        }
    }

    private void calculateTotalAmount() {
        BigDecimal total = items.stream()
                .map(item -> item.getPrice().getAmount().multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totalAmount = new DomainValueMoney(total, "USD");
    }

    public void updatePaymentStatus(SharedPaymentStatusEnum newStatus) {
        validatePaymentStatusTransition(newStatus);
        this.paymentStatus = new DomainValuePaymentStatus(newStatus);
        addDomainEvent(new OrderPaymentStatusChangedEvent(this.id, newStatus));
    }

    public void updateReservationState(SharedOrderStatusEnum newState) {
        validateReservationStateTransition(newState);
        this.reservationState = new DomainValueOrderStatus(newState);
        addDomainEvent(new OrderReservationStateChangedEvent(this.id, newState));
    }

    private void validatePaymentStatusTransition(SharedPaymentStatusEnum newStatus) {
        if (paymentStatus.getStatus() != SharedPaymentStatusEnum.PENDING) {
            throw new DomainExceptionInvalidOrder("Cannot change payment status once it's finalized");
        }
        if (!List.of(SharedPaymentStatusEnum.SUCCESS, SharedPaymentStatusEnum.FAILED, 
                     SharedPaymentStatusEnum.DECLINED).contains(newStatus)) {
            throw new DomainExceptionInvalidOrder("Invalid payment status transition");
        }
    }

    private void validateReservationStateTransition(SharedOrderStatusEnum newState) {
        SharedOrderStatusEnum currentState = reservationState.getStatus();
        if (currentState == SharedOrderStatusEnum.COMPLETED || 
            currentState == SharedOrderStatusEnum.CANCELLED) {
            throw new DomainExceptionInvalidOrder("Cannot change state of completed or cancelled order");
        }
        if (currentState == SharedOrderStatusEnum.PENDING && 
            newState != SharedOrderStatusEnum.RESERVED) {
            throw new DomainExceptionInvalidOrder("Order must be reserved before completion");
        }
    }

    public void validate() {
        if (userId == null) {
            throw new DomainExceptionInvalidOrder("User ID is required");
        }
        if (items.isEmpty()) {
            throw new DomainExceptionInvalidOrder("Order must contain at least one item");
        }
        items.forEach(this::validateItem);
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

    // Standard getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public DomainValueMoney getTotalAmount() {
        return totalAmount;
    }

    public DomainValuePaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public DomainValueOrderStatus getReservationState() {
        return reservationState;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<DomainEntityOrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainEntityOrder that = (DomainEntityOrder) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
