package ai.shreds.domain.exceptions;

import ai.shreds.domain.value_objects.DomainValueMoney;
import ai.shreds.shared.enums.SharedPaymentStatusEnum;

/**
 * Exception thrown specifically for payment failures during order processing.
 * Extends DomainPaymentException to maintain the domain exception hierarchy
 * while providing specific handling for payment failures.
 */
public class DomainExceptionPaymentFailed extends DomainPaymentException {

    private final FailureReason failureReason;

    /**
     * Enumeration of possible payment failure reasons.
     */
    public enum FailureReason {
        INSUFFICIENT_FUNDS("PAYMENT_INSUFFICIENT_FUNDS", "Insufficient funds in the account"),
        CARD_DECLINED("PAYMENT_CARD_DECLINED", "Card was declined by the issuer"),
        INVALID_CARD("PAYMENT_INVALID_CARD", "Invalid or expired card"),
        LIMIT_EXCEEDED("PAYMENT_LIMIT_EXCEEDED", "Payment limit exceeded"),
        FRAUD_SUSPECTED("PAYMENT_FRAUD_SUSPECTED", "Transaction flagged for potential fraud"),
        TECHNICAL_ERROR("PAYMENT_TECHNICAL_ERROR", "Technical error during processing"),
        SERVICE_UNAVAILABLE("PAYMENT_SERVICE_UNAVAILABLE", "Payment service temporarily unavailable"),
        UNKNOWN("PAYMENT_UNKNOWN_ERROR", "Unknown payment error");

        private final String code;
        private final String description;

        FailureReason(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Constructs a new payment failed exception with a simple message.
     *
     * @param message The error message
     * @param reason The reason for payment failure
     */
    public DomainExceptionPaymentFailed(String message, FailureReason reason) {
        super(message, PaymentError.builder()
            .errorCode(reason.getCode())
            .errorDetails(reason.getDescription())
            .status(SharedPaymentStatusEnum.FAILED)
            .build());
        this.failureReason = reason;
    }

    /**
     * Constructs a new payment failed exception with detailed payment information.
     *
     * @param message The error message
     * @param reason The reason for payment failure
     * @param paymentId The ID of the failed payment
     * @param paymentMethod The payment method used
     * @param amount The amount that failed to process
     */
    public DomainExceptionPaymentFailed(String message, FailureReason reason,
                                        String paymentId, String paymentMethod, DomainValueMoney amount) {
        super(message, PaymentError.builder()
            .paymentId(paymentId)
            .paymentMethod(paymentMethod)
            .amount(amount)
            .status(SharedPaymentStatusEnum.FAILED)
            .errorCode(reason.getCode())
            .errorDetails(reason.getDescription())
            .build());
        this.failureReason = reason;
    }

    /**
     * Creates a new builder for constructing payment failed exceptions.
     *
     * @return A new builder instance
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Gets the reason for the payment failure.
     *
     * @return The failure reason
     */
    public FailureReason getFailureReason() {
        return failureReason;
    }

    /**
     * Builder for creating payment failed exceptions with detailed information.
     */
    public static class Builder {
        private String message;
        private FailureReason reason = FailureReason.UNKNOWN;
        private String paymentId;
        private String paymentMethod;
        private DomainValueMoney amount;

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder reason(FailureReason reason) {
            this.reason = reason;
            return this;
        }

        public Builder paymentId(String paymentId) {
            this.paymentId = paymentId;
            return this;
        }

        public Builder paymentMethod(String paymentMethod) {
            this.paymentMethod = paymentMethod;
            return this;
        }

        public Builder amount(DomainValueMoney amount) {
            this.amount = amount;
            return this;
        }

        public DomainExceptionPaymentFailed build() {
            if (paymentId == null || paymentMethod == null || amount == null) {
                return new DomainExceptionPaymentFailed(message, reason);
            }
            return new DomainExceptionPaymentFailed(message, reason,
                paymentId, paymentMethod, amount);
        }
    }

    @Override
    public String toString() {
        return String.format(
            "%s%nFailure Reason: %s - %s",
            super.toString(),
            failureReason.getCode(),
            failureReason.getDescription()
        );
    }
}