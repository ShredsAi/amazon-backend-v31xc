package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainEntityOrder;
import ai.shreds.domain.exceptions.DomainExceptionInvalidOrder;
import ai.shreds.domain.exceptions.DomainInventoryException;
import ai.shreds.domain.exceptions.DomainPaymentException;
import ai.shreds.domain.exceptions.DomainOrderException;

/**
 * Primary domain port for order creation. This port defines the contract for creating
 * new orders within the domain layer, ensuring all business rules and validations
 * are properly enforced.
 */
public interface DomainInputPortCreateOrder {

    /**
     * Creates a new order in the system, coordinating inventory reservation,
     * payment processing, and order persistence.
     *
     * @param order The order to be created, containing:
     *             - User ID (required)
     *             - List of order items with product IDs, quantities, and prices (at least one required)
     *             - Payment method (required)
     *             - Initial payment status (PENDING)
     *             - Initial reservation state (PENDING)
     *
     * @return The processed order with:
     *         - Generated order ID
     *         - Updated payment status
     *         - Updated reservation state
     *         - Creation timestamp
     *         - Processed items list
     *         - Final total amount
     *
     * @throws DomainExceptionInvalidOrder if the order fails validation:
     *         - Missing required fields
     *         - Invalid quantities
     *         - Invalid prices
     *         - Empty items list
     * @throws DomainInventoryException if inventory reservation fails:
     *         - Insufficient stock
     *         - Product not found
     *         - Inventory service error
     * @throws DomainPaymentException if payment processing fails:
     *         - Payment declined
     *         - Payment service error
     *         - Invalid payment details
     * @throws DomainOrderException for other order processing errors:
     *         - Repository errors
     *         - System errors
     *         - Unexpected states
     */
    DomainEntityOrder execute(DomainEntityOrder order) throws 
        DomainExceptionInvalidOrder,
        DomainInventoryException,
        DomainPaymentException,
        DomainOrderException;
}
