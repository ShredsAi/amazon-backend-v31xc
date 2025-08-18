package ai.shreds.domain.entities;

import ai.shreds.domain.events.DomainEvent;
import ai.shreds.domain.events.OrderPaymentStatusChangedEvent;
import ai.shreds.domain.events.OrderReservationStateChangedEvent;
import ai.shreds.domain.exceptions.DomainExceptionInvalidOrder;
import ai.shreds.domain.value_objects.DomainValueMoney;
import ai.shreds.domain.value_objects.DomainValueOrderStatus;
import ai.shreds.domain.value_objects.DomainValuePaymentStatus;
import ai.shreds.shared.enums.SharedOrderStatusEnum;
import ai.shreds.shared.enums.SharedPaymentStatusEnum;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class DomainOrderEntity {

    private Long id;
    private Long userId;
    private DomainValueMoney totalAmount;
    private DomainValuePaymentStatus paymentStatus;
    private DomainValueOrderStatus reservationState;
    private LocalDateTime createdAt;
    private List<DomainOrderItemEntity> items;
    private List<DomainEvent> domainEvents;
    private String paymentMethod;

    protected DomainOrderEntity() {
        this.items = new ArrayList<>();
        this.domainEvents = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.paymentStatus = DomainValuePaymentStatus.initial();
        this.reservationState = DomainValueOrderStatus.initial();
    }

    public DomainOrderEntity(Long userId, String paymentMethod) {
        this();
        validateUserId(userId);
        validatePaymentMethod(paymentMethod);
        this.userId = userId;
        this.paymentMethod = paymentMethod;
        this.totalAmount = DomainValueMoney.zero("USD"); // Default currency
    }

    public void updatePaymentStatus(SharedPaymentStatusEnum newStatus) {
        DomainValuePaymentStatus oldStatus = this.paymentStatus;
        this.paymentStatus = this.paymentStatus.transition(newStatus);
        addDomainEvent(new OrderPaymentStatusChangedEvent(
            this.id,
            oldStatus.getStatus(),
            newStatus
        ));
    }

    public void updateReservationState(SharedOrderStatusEnum newState) {
        DomainValueOrderStatus oldState = this.reservationState;
        this.reservationState = this.reservationState.transition(newState);
        addDomainEvent(new OrderReservationStateChangedEvent(
            this.id,
            oldState.getStatus(),
            newState
        ));
    }

    // ... rest of the code remains the same ...

    public void addItem(DomainOrderItemEntity item) {
        validateItem(item);
        items.add(item);
        calculateTotalAmount();
    }

    public void removeItem(DomainOrderItemEntity item) {
        items.remove(item);
        calculateTotalAmount();
    }

    private void validateItem(DomainOrderItemEntity item) {
        if (item == null) {
            throw new DomainExceptionInvalidOrder("Order item cannot be null");
        }
        if (item.getQuantity() <= 0) {
            throw new DomainExceptionInvalidOrder("Item quantity must be positive");
        }
        if (item.getPrice() == null || item.getPrice().isZero()) {
            throw new DomainExceptionInvalidOrder("Item price must be positive");
        }
    }

    private void calculateTotalAmount() {
        this.totalAmount = items.stream()
                .map(item -> item.getPrice().multiply(item.getQuantity()))
                .reduce(DomainValueMoney.zero("USD"), DomainValueMoney::add);
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new DomainExceptionInvalidOrder("Invalid user ID");
        }
    }

    private void validatePaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
            throw new DomainExceptionInvalidOrder("Payment method is required");
        }
    }

    public void validate() {
        validateUserId(this.userId);
        validatePaymentMethod(this.paymentMethod);
        if (items.isEmpty()) {
            throw new DomainExceptionInvalidOrder("Order must contain at least one item");
        }
        items.forEach(this::validateItem);
        if (totalAmount.isZero()) {
            throw new DomainExceptionInvalidOrder("Order total amount must be greater than zero");
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

    // Getters
    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
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

    public List<DomainOrderItemEntity> getItems() {
        return Collections.unmodifiableList(items);
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    void setId(Long id) {
        this.id = id;
    }

    // Status checks
    public boolean isPending() {
        return reservationState.isPending();
    }

    public boolean isReserved() {
        return reservationState.isReserved();
    }

    public boolean isCompleted() {
        return reservationState.isCompleted();
    }

    public boolean isCancelled() {
        return reservationState.isCancelled();
    }

    public boolean isPaymentPending() {
        return paymentStatus.isPending();
    }

    public boolean isPaymentSuccess() {
        return paymentStatus.isSuccess();
    }

    public boolean isPaymentFailed() {
        return paymentStatus.isFailed();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainOrderEntity that = (DomainOrderEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Order[id=%d, userId=%d, total=%s, paymentStatus=%s, reservationState=%s]",
            id, userId, totalAmount, paymentStatus, reservationState);
    }
}