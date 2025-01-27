package ai.shreds.domain.value_objects;

import ai.shreds.domain.exceptions.DomainExceptionInvalidOrder;
import ai.shreds.shared.enums.SharedOrderStatusEnum;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class DomainValueOrderStatus {

    private final SharedOrderStatusEnum status;

    private static final Map<SharedOrderStatusEnum, List<SharedOrderStatusEnum>> VALID_TRANSITIONS;

    static {
        VALID_TRANSITIONS = new EnumMap<>(SharedOrderStatusEnum.class);
        VALID_TRANSITIONS.put(SharedOrderStatusEnum.PENDING, 
            List.of(SharedOrderStatusEnum.RESERVED, SharedOrderStatusEnum.CANCELLED));
        VALID_TRANSITIONS.put(SharedOrderStatusEnum.RESERVED, 
            List.of(SharedOrderStatusEnum.COMPLETED, SharedOrderStatusEnum.CANCELLED));
        VALID_TRANSITIONS.put(SharedOrderStatusEnum.COMPLETED, List.of());
        VALID_TRANSITIONS.put(SharedOrderStatusEnum.CANCELLED, List.of());
    }

    private DomainValueOrderStatus(SharedOrderStatusEnum status) {
        validateStatus(status);
        this.status = status;
    }

    public static DomainValueOrderStatus of(SharedOrderStatusEnum status) {
        return new DomainValueOrderStatus(status);
    }

    public static DomainValueOrderStatus initial() {
        return new DomainValueOrderStatus(SharedOrderStatusEnum.PENDING);
    }

    private void validateStatus(SharedOrderStatusEnum status) {
        if (status == null) {
            throw new DomainExceptionInvalidOrder("Order status cannot be null");
        }
    }

    public boolean canTransitionTo(SharedOrderStatusEnum newStatus) {
        if (newStatus == null) {
            return false;
        }
        return VALID_TRANSITIONS.get(this.status).contains(newStatus);
    }

    public DomainValueOrderStatus transition(SharedOrderStatusEnum newStatus) {
        if (!canTransitionTo(newStatus)) {
            throw new DomainExceptionInvalidOrder(
                String.format("Invalid status transition from %s to %s", 
                    this.status, newStatus)
            );
        }
        return new DomainValueOrderStatus(newStatus);
    }

    public boolean isPending() {
        return status == SharedOrderStatusEnum.PENDING;
    }

    public boolean isReserved() {
        return status == SharedOrderStatusEnum.RESERVED;
    }

    public boolean isCompleted() {
        return status == SharedOrderStatusEnum.COMPLETED;
    }

    public boolean isCancelled() {
        return status == SharedOrderStatusEnum.CANCELLED;
    }

    public boolean isTerminal() {
        return isCompleted() || isCancelled();
    }

    public SharedOrderStatusEnum getStatus() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainValueOrderStatus that = (DomainValueOrderStatus) o;
        return status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(status);
    }

    @Override
    public String toString() {
        return status.toString();
    }
}
