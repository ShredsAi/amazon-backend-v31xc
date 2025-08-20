package ai.shreds.domain.exceptions;

import ai.shreds.domain.value_objects.DomainValueMoney;
import ai.shreds.shared.enums.SharedPaymentStatusEnum;

/**
 * Exception thrown when payment processing fails during order creation.
 * Extends DomainOrderException to maintain the domain exception hierarchy.
 */
public class DomainPaymentException extends DomainOrderException {

    private final PaymentError paymentError;

    /**
     * Represents detailed payment error information.
     */
    public static class PaymentError {
        private final String paymentId;
        private final String paymentMethod;
        private final DomainValueMoney amount;
        private final SharedPaymentStatusEnum status;
        private final String errorCode;
        private final String errorDetails;

        private PaymentError(Builder builder) {
            this.paymentId = builder.paymentId;
            this.paymentMethod = builder.paymentMethod;
            this.amount = builder.amount;
            this.status = builder.status;
            this.errorCode = builder.errorCode;
            this.errorDetails = builder.errorDetails;
        }

        public String getPaymentId() { return paymentId; }
        public String getPaymentMethod() { return paymentMethod; }
        public DomainValueMoney getAmount() { return amount; }
        public SharedPaymentStatusEnum getStatus() { return status; }
        public String getErrorCode() { return errorCode; }
        public String getErrorDetails() { return errorDetails; }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String paymentId;
            private String paymentMethod;
            private DomainValueMoney amount;
            private SharedPaymentStatusEnum status;
            private String errorCode;
            private String errorDetails;

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

            public Builder status(SharedPaymentStatusEnum status) {
                this.status = status;
                return this;
            }

            public Builder errorCode(String errorCode) {
                this.errorCode = errorCode;
                return this;
            }

            public Builder errorDetails(String errorDetails) {
                this.errorDetails = errorDetails;
                return this;
            }

            public PaymentError build() {
                return new PaymentError(this);
            }
        }

        @Override
        public String toString() {
            return String.format(
                "Payment Error [ID: %s, Method: %s, Amount: %s, Status: %s, Code: %s, Details: %s]",
                paymentId, paymentMethod, amount, status, errorCode, errorDetails
            );
        }
    }

    /**
     * Constructs a new payment exception with a simple message.
     *
     * @param message The error message
     */
    public DomainPaymentException(String message) {
        super(message, "PAYMENT_ERROR");
        this.paymentError = PaymentError.builder()
            .errorDetails(message)
            .errorCode("GENERAL_ERROR")
            .build();
    }

    /**
     * Constructs a new payment exception with detailed payment error information.
     *
     * @param message The error message
     * @param paymentError Detailed payment error information
     */
    public DomainPaymentException(String message, PaymentError paymentError) {
        super(message, "PAYMENT_ERROR_" + paymentError.getErrorCode());
        this.paymentError = paymentError;
    }

    /**
     * Constructs a new payment exception with a cause.
     *
     * @param message The error message
     * @param cause The cause of the exception
     * @param paymentError Detailed payment error information
     */
    public DomainPaymentException(String message, Throwable cause, PaymentError paymentError) {
        super(message, "PAYMENT_ERROR_" + paymentError.getErrorCode(), cause);
        this.paymentError = paymentError;
    }

    /**
     * Gets the detailed payment error information.
     *
     * @return The payment error details
     */
    public PaymentError getPaymentError() {
        return paymentError;
    }

    @Override
    public String toString() {
        return String.format("%s%nPayment Details: %s", 
            super.toString(), 
            paymentError.toString());
    }

    /**
     * Creates a builder for constructing payment exceptions with detailed error information.
     *
     * @return A new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating payment exceptions with detailed information.
     */
    public static class Builder {
        private String message;
        private Throwable cause;
        private PaymentError.Builder paymentErrorBuilder = PaymentError.builder();

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder cause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        public Builder paymentId(String paymentId) {
            paymentErrorBuilder.paymentId(paymentId);
            return this;
        }

        public Builder paymentMethod(String paymentMethod) {
            paymentErrorBuilder.paymentMethod(paymentMethod);
            return this;
        }

        public Builder amount(DomainValueMoney amount) {
            paymentErrorBuilder.amount(amount);
            return this;
        }

        public Builder status(SharedPaymentStatusEnum status) {
            paymentErrorBuilder.status(status);
            return this;
        }

        public Builder errorCode(String errorCode) {
            paymentErrorBuilder.errorCode(errorCode);
            return this;
        }

        public Builder errorDetails(String errorDetails) {
            paymentErrorBuilder.errorDetails(errorDetails);
            return this;
        }

        public DomainPaymentException build() {
            PaymentError error = paymentErrorBuilder.build();
            return cause != null ?
                new DomainPaymentException(message, cause, error) :
                new DomainPaymentException(message, error);
        }
    }
}
