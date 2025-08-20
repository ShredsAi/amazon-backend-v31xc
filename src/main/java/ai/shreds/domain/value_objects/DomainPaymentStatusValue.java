package ai.shreds.domain.value_objects;

import ai.shreds.domain.exceptions.DomainExceptionInvalidOrder;
import ai.shreds.shared.enums.SharedPaymentStatusEnum;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Value object representing the payment status in the domain.
 * Implements immutability and proper value object semantics.
 */
public final class DomainPaymentStatusValue {

    private final SharedPaymentStatusEnum status;

    private static final Map<SharedPaymentStatusEnum, List<SharedPaymentStatusEnum>> VALID_TRANSITIONS;

    static {
        VALID_TRANSITIONS = new EnumMap<>(SharedPaymentStatusEnum.class);
        VALID_TRANSITIONS.put(SharedPaymentStatusEnum.PENDING, 
            List.of(SharedPaymentStatusEnum.SUCCESS, SharedPaymentStatusEnum.FAILED, 
                    SharedPaymentStatusEnum.DECLINED));
        VALID_TRANSITIONS.put(SharedPaymentStatusEnum.SUCCESS, List.of());
        VALID_TRANSITIONS.put(SharedPaymentStatusEnum.FAILED, List.of());
        VALID_TRANSITIONS.put(SharedPaymentStatusEnum.DECLINED, List.of());
    }

    private DomainPaymentStatusValue(SharedPaymentStatusEnum status) {
        validateStatus(status);
        this.status = status;
    }

    /**
     * Creates a new instance with the given status.
     *
     * @param status The payment status
     * @return A new DomainPaymentStatusValue instance
     * @throws DomainExceptionInvalidOrder if status is invalid
     */
    public static DomainPaymentStatusValue of(SharedPaymentStatusEnum status) {
        return new DomainPaymentStatusValue(status);
    }

    /**
     * Creates a new instance with initial PENDING status.
     *
     * @return A new DomainPaymentStatusValue instance with PENDING status
     */
    public static DomainPaymentStatusValue initial() {
        return new DomainPaymentStatusValue(SharedPaymentStatusEnum.PENDING);
    }

    private void validateStatus(SharedPaymentStatusEnum status) {
        if (status == null) {
            throw new DomainExceptionInvalidOrder("Payment status cannot be null");
        }
    }

    /**
     * Checks if transition to the given status is valid.
     *
     * @param newStatus The target status
     * @return true if transition is valid, false otherwise
     */
    public boolean canTransitionTo(SharedPaymentStatusEnum newStatus) {
        if (newStatus == null) {
            return false;
        }
        return VALID_TRANSITIONS.get(this.status).contains(newStatus);
    }

    /**
     * Creates a new instance with the new status if transition is valid.
     *
     * @param newStatus The target status
     * @return A new DomainPaymentStatusValue instance with the new status
     * @throws DomainExceptionInvalidOrder if transition is invalid
     */
    public DomainPaymentStatusValue transition(SharedPaymentStatusEnum newStatus) {
        if (!canTransitionTo(newStatus)) {
            throw new DomainExceptionInvalidOrder(
                String.format("Invalid payment status transition from %s to %s", 
                    this.status, newStatus));
        }
        return new DomainPaymentStatusValue(newStatus);
    }

    /**
     * Gets the current status value.
     *
     * @return The current status
     */
    public SharedPaymentStatusEnum getStatus() {
        return status;
    }

    /**
     * Checks if the payment is in PENDING status.
     *
     * @return true if status is PENDING
     */
    public boolean isPending() {
        return status == SharedPaymentStatusEnum.PENDING;
    }

    /**
     * Checks if the payment is in SUCCESS status.
     *
     * @return true if status is SUCCESS
     */
    public boolean isSuccess() {
        return status == SharedPaymentStatusEnum.SUCCESS;
    }

    /**
     * Checks if the payment is in FAILED status.
     *
     * @return true if status is FAILED
     */
    public boolean isFailed() {
        return status == SharedPaymentStatusEnum.FAILED;
    }

    /**
     * Checks if the payment is in DECLINED status.
     *
     * @return true if status is DECLINED
     */
    public boolean isDeclined() {
        return status == SharedPaymentStatusEnum.DECLINED;
    }

    /**
     * Checks if the payment is in a terminal state (SUCCESS, FAILED, or DECLINED).
     *
     * @return true if status is terminal
     */
    public boolean isTerminal() {
        return isSuccess() || isFailed() || isDeclined();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainPaymentStatusValue that = (DomainPaymentStatusValue) o;
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
