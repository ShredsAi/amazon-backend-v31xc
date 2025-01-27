package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainEntityPaymentDetails;
import ai.shreds.domain.exceptions.DomainPaymentException;
import ai.shreds.domain.value_objects.DomainValueMoney;

import java.util.Optional;

/**
 * Domain output port for payment processing operations. This interface defines
 * the contract for processing payments and managing payment status during order
 * processing, maintaining a clean separation between domain logic and external
 * payment systems.
 */
public interface DomainOutputPortPaymentService {

    /**
     * Processes a payment through the external payment service.
     *
     * @param paymentDetails Payment details containing amount, method, and order reference
     * @return Updated payment details with processing result and payment status
     * @throws DomainPaymentException if payment processing fails due to:
     *         - Invalid payment details
     *         - Payment declined
     *         - Communication error with payment service
     *         - Timeout during processing
     */
    DomainEntityPaymentDetails processPayment(DomainEntityPaymentDetails paymentDetails) 
        throws DomainPaymentException;

    /**
     * Validates payment details before processing.
     * This can be used to check if the payment method is valid and the amount is within limits.
     *
     * @param paymentDetails Payment details to validate
     * @return true if payment details are valid, false otherwise
     * @throws DomainPaymentException if validation fails
     */
    boolean validatePaymentDetails(DomainEntityPaymentDetails paymentDetails) 
        throws DomainPaymentException;

    /**
     * Retrieves the current status of a payment.
     *
     * @param paymentId Unique identifier of the payment
     * @return Optional containing payment details if found
     * @throws DomainPaymentException if status check fails
     */
    Optional<DomainEntityPaymentDetails> getPaymentStatus(String paymentId) 
        throws DomainPaymentException;

    /**
     * Cancels a pending payment.
     * This should only be possible for payments that haven't been processed yet.
     *
     * @param paymentId Unique identifier of the payment to cancel
     * @throws DomainPaymentException if cancellation fails or payment is already processed
     */
    void cancelPayment(String paymentId) throws DomainPaymentException;

    /**
     * Refunds a processed payment.
     * This should only be possible for successful payments.
     *
     * @param paymentId Unique identifier of the payment to refund
     * @param amount Amount to refund, must not exceed original payment amount
     * @return Updated payment details including refund status
     * @throws DomainPaymentException if refund fails or payment cannot be refunded
     */
    DomainEntityPaymentDetails refundPayment(String paymentId, DomainValueMoney amount) 
        throws DomainPaymentException;

    /**
     * Verifies if a payment method is supported and active.
     *
     * @param paymentMethod The payment method to verify
     * @return true if payment method is supported and active
     * @throws DomainPaymentException if verification fails
     */
    boolean isPaymentMethodSupported(String paymentMethod) throws DomainPaymentException;

    /**
     * Authorizes a payment without capturing it.
     * This can be used to verify payment method validity before processing.
     *
     * @param paymentDetails Payment details to authorize
     * @return true if authorization succeeds
     * @throws DomainPaymentException if authorization fails
     */
    boolean authorizePayment(DomainEntityPaymentDetails paymentDetails) 
        throws DomainPaymentException;

    /**
     * Captures a previously authorized payment.
     *
     * @param paymentId Unique identifier of the authorized payment
     * @return Updated payment details after capture
     * @throws DomainPaymentException if capture fails or payment wasn't authorized
     */
    DomainEntityPaymentDetails capturePayment(String paymentId) throws DomainPaymentException;
}
