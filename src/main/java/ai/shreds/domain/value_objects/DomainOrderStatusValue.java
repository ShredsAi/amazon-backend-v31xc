package ai.shreds.domain.value_objects;

import ai.shreds.domain.exceptions.DomainExceptionInvalidOrder;
import ai.shreds.shared.enums.SharedOrderStatusEnum;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Value object representing the status of an order in the domain.
 * Implements immutability and proper value object semantics.
 */
public final class DomainOrderStatusValue {

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

    private DomainOrderStatusValue(SharedOrderStatusEnum status) {
        validateStatus(status);
        this.status = status;
    }

    /**
     * Creates a new instance with the given status.
     *
     * @param status The order status
     * @return A new DomainOrderStatusValue instance
     * @throws DomainExceptionInvalidOrder if status is invalid
     */
    public static DomainOrderStatusValue of(SharedOrderStatusEnum status) {
        return new DomainOrderStatusValue(status);
    }

    /**
     * Creates a new instance with initial PENDING status.
     *
     * @return A new DomainOrderStatusValue instance with PENDING status
     */
    public static DomainOrderStatusValue initial() {
        return new DomainOrderStatusValue(SharedOrderStatusEnum.PENDING);
    }

    private void validateStatus(SharedOrderStatusEnum status) {
        if (status == null) {
            throw new DomainExceptionInvalidOrder("Order status cannot be null");
        }
    }

    /**
     * Checks if transition to the given status is valid.
     *
     * @param newStatus The target status
     * @return true if transition is valid, false otherwise
     */
    public boolean canTransitionTo(SharedOrderStatusEnum newStatus) {
        if (newStatus == null) {
            return false;
        }
        return VALID_TRANSITIONS.get(this.status).contains(newStatus);
    }

    /**
     * Creates a new instance with the new status if transition is valid.
     *
     * @param newStatus The target status
     * @return A new DomainOrderStatusValue instance with the new status
     * @throws DomainExceptionInvalidOrder if transition is invalid
     */
    public DomainOrderStatusValue transition(SharedOrderStatusEnum newStatus) {
        if (!canTransitionTo(newStatus)) {
            throw new DomainExceptionInvalidOrder(
                String.format("Invalid status transition from %s to %s", 
                    this.status, newStatus));
        }
        return new DomainOrderStatusValue(newStatus);
    }

    /**
     * Gets the current status value.
     *
     * @return The current status
     */
    public SharedOrderStatusEnum getStatus() {
        return status;
    }

    /**
     * Checks if the order is in PENDING status.
     *
     * @return true if status is PENDING
     */
    public boolean isPending() {
        return status == SharedOrderStatusEnum.PENDING;
    }

    /**
     * Checks if the order is in RESERVED status.
     *
     * @return true if status is RESERVED
     */
    public boolean isReserved() {
        return status == SharedOrderStatusEnum.RESERVED;
    }

    /**
     * Checks if the order is in COMPLETED status.
     *
     * @return true if status is COMPLETED
     */
    public boolean isCompleted() {
        return status == SharedOrderStatusEnum.COMPLETED;
    }

    /**
     * Checks if the order is in CANCELLED status.
     *
     * @return true if status is CANCELLED
     */
    public boolean isCancelled() {
        return status == SharedOrderStatusEnum.CANCELLED;
    }

    /**
     * Checks if the order is in a terminal state (COMPLETED or CANCELLED).
     *
     * @return true if status is terminal
     */
    public boolean isTerminal() {
        return isCompleted() || isCancelled();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainOrderStatusValue that = (DomainOrderStatusValue) o;
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
