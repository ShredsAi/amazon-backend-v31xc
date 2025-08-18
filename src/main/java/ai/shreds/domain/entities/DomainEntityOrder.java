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
    private String paymentMethod;

    public DomainEntityOrder() {
        this.items = new ArrayList<>();
        this.domainEvents = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.paymentStatus = DomainValuePaymentStatus.of(SharedPaymentStatusEnum.PENDING);
        this.reservationState = DomainValueOrderStatus.of(SharedOrderStatusEnum.PENDING);
        this.totalAmount = DomainValueMoney.of(BigDecimal.ZERO, "USD");
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public void updatePaymentStatus(SharedPaymentStatusEnum newStatus) {
        SharedPaymentStatusEnum oldStatus = this.paymentStatus.getStatus();
        this.paymentStatus = DomainValuePaymentStatus.of(newStatus);
        this.addDomainEvent(new OrderPaymentStatusChangedEvent(this.id, oldStatus, newStatus));
    }

    public void updateReservationState(SharedOrderStatusEnum newState) {
        SharedOrderStatusEnum oldState = this.reservationState.getStatus();
        this.reservationState = DomainValueOrderStatus.of(newState);
        this.addDomainEvent(new OrderReservationStateChangedEvent(this.id, oldState, newState));
    }

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

    public void setTotalAmount(DomainValueMoney totalAmount) {
        this.totalAmount = totalAmount;
    }

    public DomainValuePaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(DomainValuePaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public DomainValueOrderStatus getReservationState() {
        return reservationState;
    }

    public void setReservationState(DomainValueOrderStatus reservationState) {
        this.reservationState = reservationState;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<DomainEntityOrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void addItem(DomainEntityOrderItem item) {
        if (item != null) {
            this.items.add(item);
        }
    }

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void addDomainEvent(DomainEvent event) {
        if (event != null) {
            this.domainEvents.add(event);
        }
    }

    public void clearDomainEvents() {
        this.domainEvents.clear();
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

    @Override
    public String toString() {
        return "DomainEntityOrder{" +
                "id=" + id +
                ", userId=" + userId +
                ", totalAmount=" + totalAmount +
                ", paymentStatus=" + paymentStatus +
                ", reservationState=" + reservationState +
                ", createdAt=" + createdAt +
                '}';
    }
}