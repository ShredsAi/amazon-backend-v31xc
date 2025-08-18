package ai.shreds.domain.value_objects;

import ai.shreds.shared.enums.SharedPaymentStatusEnum;
import lombok.Value;

@Value
public class DomainValuePaymentStatus {
    SharedPaymentStatusEnum status;

    private DomainValuePaymentStatus(SharedPaymentStatusEnum status) {
        this.status = status;
    }

    public static DomainValuePaymentStatus of(SharedPaymentStatusEnum status) {
        return new DomainValuePaymentStatus(status);
    }

    public static DomainValuePaymentStatus initial() {
        return new DomainValuePaymentStatus(SharedPaymentStatusEnum.PENDING);
    }

    public SharedPaymentStatusEnum getStatus() {
        return status;
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

    public DomainValuePaymentStatus transition(SharedPaymentStatusEnum newStatus) {
        return DomainValuePaymentStatus.of(newStatus);
    }
}