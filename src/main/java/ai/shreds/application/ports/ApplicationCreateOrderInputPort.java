package ai.shreds.application.ports;

import ai.shreds.shared.dtos.SharedCreateOrderRequest;
import ai.shreds.shared.dtos.SharedOrderResponse;
import ai.shreds.application.exceptions.ApplicationOrderValidationException;
import ai.shreds.application.exceptions.ApplicationOrderCreationException;

/**
 * Input port for order creation operations in the application layer.
 * This port defines the contract for creating new orders in the system.
 */
public interface ApplicationCreateOrderInputPort {

    /**
     * Creates a new order based on the provided request.
     *
     * @param request The order creation request containing necessary information
     * @return SharedOrderResponse containing the created order details
     * @throws ApplicationOrderValidationException if the request fails validation
     * @throws ApplicationOrderCreationException if there's an error during order creation
     */
    SharedOrderResponse createOrder(SharedCreateOrderRequest request);

    /**
     * Validates the order creation request.
     *
     * @param request The order creation request to validate
     * @throws ApplicationOrderValidationException if the request is invalid
     */
    default void validateOrderRequest(SharedCreateOrderRequest request) {
        if (request == null) {
            throw new ApplicationOrderValidationException("Order request cannot be null", "ERR-400");
        }
        if (request.getUserId() == null) {
            throw new ApplicationOrderValidationException("User ID is required", "ERR-400");
        }
        if (request.getPaymentMethod() == null || request.getPaymentMethod().trim().isEmpty()) {
            throw new ApplicationOrderValidationException("Payment method is required", "ERR-400");
        }
    }
}
