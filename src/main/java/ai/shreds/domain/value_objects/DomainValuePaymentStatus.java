package ai.shreds.domain.value_objects;

import ai.shreds.domain.exceptions.DomainPaymentException;
import ai.shreds.shared.enums.SharedPaymentStatusEnum;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class DomainValuePaymentStatus {

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

    private DomainValuePaymentStatus(SharedPaymentStatusEnum status) {
        validateStatus(status);
        this.status = status;
    }

    public static DomainValuePaymentStatus of(SharedPaymentStatusEnum status) {
        return new DomainValuePaymentStatus(status);
    }

    public static DomainValuePaymentStatus initial() {
        return new DomainValuePaymentStatus(SharedPaymentStatusEnum.PENDING);
    }

    private void validateStatus(SharedPaymentStatusEnum status) {
        if (status == null) {
            throw new DomainPaymentException("Payment status cannot be null");
        }
    }

    public boolean canTransitionTo(SharedPaymentStatusEnum newStatus) {
        if (newStatus == null) {
            return false;
        }
        return VALID_TRANSITIONS.get(this.status).contains(newStatus);
    }

    public DomainValuePaymentStatus transition(SharedPaymentStatusEnum newStatus) {
        if (!canTransitionTo(newStatus)) {
            throw new DomainPaymentException(
                String.format("Invalid payment status transition from %s to %s", 
                    this.status, newStatus)
            );
        }
        return new DomainValuePaymentStatus(newStatus);
    }

    public boolean isPending() {
        return status == SharedPaymentStatusEnum.PENDING;
    }

    public boolean isSuccess() {
        return status == SharedPaymentStatusEnum.SUCCESS;
    }

    public boolean isFailed() {
        return status == SharedPaymentStatusEnum.FAILED;
    }

    public boolean isDeclined() {
        return status == SharedPaymentStatusEnum.DECLINED;
    }

    public boolean isTerminal() {
        return isSuccess() || isFailed() || isDeclined();
    }

    public SharedPaymentStatusEnum getStatus() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainValuePaymentStatus that = (DomainValuePaymentStatus) o;
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
