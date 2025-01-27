package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainEntityOrderItem;
import ai.shreds.domain.exceptions.DomainInventoryException;

import java.util.List;
import java.util.Map;

/**
 * Domain output port for inventory management operations. This interface defines
 * the contract for reserving and managing inventory during order processing,
 * maintaining a clean separation between domain logic and external inventory systems.
 */
public interface DomainOutputPortInventoryService {

    /**
     * Attempts to reserve inventory for all items in the order.
     * This operation should be atomic - either all items are reserved or none.
     *
     * @param items List of order items to reserve inventory for
     * @return true if all items were successfully reserved, false otherwise
     * @throws DomainInventoryException if reservation fails due to:
     *         - Insufficient stock
     *         - Product not found
     *         - Communication error with inventory service
     *         - Timeout during reservation attempt
     */
    boolean reserveItems(List<DomainEntityOrderItem> items) throws DomainInventoryException;

    /**
     * Releases previously reserved inventory items.
     * This method should be called during rollback scenarios or when order processing fails.
     *
     * @param items List of order items whose inventory should be released
     * @throws DomainInventoryException if release operation fails
     */
    void releaseItems(List<DomainEntityOrderItem> items) throws DomainInventoryException;

    /**
     * Checks if sufficient inventory is available for the specified items
     * without actually reserving it.
     *
     * @param items List of order items to check availability for
     * @return Map of productId to boolean indicating availability status
     * @throws DomainInventoryException if availability check fails
     */
    Map<Long, Boolean> checkAvailability(List<DomainEntityOrderItem> items) throws DomainInventoryException;

    /**
     * Gets the current inventory levels for specified products.
     *
     * @param productIds List of product IDs to check
     * @return Map of productId to current available quantity
     * @throws DomainInventoryException if inventory check fails
     */
    Map<Long, Integer> getInventoryLevels(List<Long> productIds) throws DomainInventoryException;

    /**
     * Validates that all products in the order items exist and are active.
     *
     * @param items List of order items to validate
     * @return true if all products exist and are active
     * @throws DomainInventoryException if validation fails or products don't exist
     */
    boolean validateProducts(List<DomainEntityOrderItem> items) throws DomainInventoryException;

    /**
     * Confirms a reservation, marking it as permanent in the inventory system.
     * This should be called after successful payment processing.
     *
     * @param orderId The ID of the order whose reservations should be confirmed
     * @param items List of order items whose reservations should be confirmed
     * @throws DomainInventoryException if confirmation fails
     */
    void confirmReservation(Long orderId, List<DomainEntityOrderItem> items) throws DomainInventoryException;

    /**
     * Cancels a confirmed reservation, returning items to inventory.
     * This should be called during order cancellation.
     *
     * @param orderId The ID of the order whose reservations should be cancelled
     * @param items List of order items whose reservations should be cancelled
     * @throws DomainInventoryException if cancellation fails
     */
    void cancelReservation(Long orderId, List<DomainEntityOrderItem> items) throws DomainInventoryException;
}
