package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainEntityOrder;
import ai.shreds.domain.entities.DomainEntityOrderItem;
import ai.shreds.domain.exceptions.DomainOrderException;

import java.util.List;
import java.util.Optional;

/**
 * Domain output port for order persistence operations. This interface defines
 * the contract for storing and retrieving orders and their items, maintaining
 * a clean separation between domain and infrastructure layers.
 */
public interface DomainOutputPortOrderRepository {

    /**
     * Persists or updates an order entity.
     *
     * @param order The order to be saved or updated
     * @return The persisted order with generated ID and any updated fields
     * @throws DomainOrderException if the save operation fails
     */
    DomainEntityOrder save(DomainEntityOrder order) throws DomainOrderException;

    /**
     * Retrieves an order by its unique identifier.
     *
     * @param id The unique identifier of the order
     * @return Optional containing the order if found, empty Optional if not
     * @throws DomainOrderException if the retrieval operation fails
     */
    Optional<DomainEntityOrder> findById(Long id) throws DomainOrderException;

    /**
     * Persists a collection of order items associated with an order.
     * All items in the list must belong to the same order.
     *
     * @param items The list of order items to be saved
     * @throws DomainOrderException if the save operation fails or if items belong to different orders
     */
    void saveItems(List<DomainEntityOrderItem> items) throws DomainOrderException;

    /**
     * Retrieves all items belonging to a specific order.
     *
     * @param orderId The ID of the order whose items should be retrieved
     * @return List of order items associated with the order
     * @throws DomainOrderException if the retrieval operation fails
     */
    List<DomainEntityOrderItem> findItemsByOrderId(Long orderId) throws DomainOrderException;

    /**
     * Deletes an order and all its associated items.
     * This operation should be used with caution and only in specific business scenarios.
     *
     * @param orderId The ID of the order to be deleted
     * @throws DomainOrderException if the deletion operation fails or if the order doesn't exist
     */
    void deleteOrder(Long orderId) throws DomainOrderException;

    /**
     * Updates the status of multiple order items in a single transaction.
     * Useful for batch operations like updating inventory status.
     *
     * @param items The list of order items to be updated
     * @throws DomainOrderException if the update operation fails
     */
    void updateItems(List<DomainEntityOrderItem> items) throws DomainOrderException;

    /**
     * Checks if an order exists in the repository.
     *
     * @param orderId The ID of the order to check
     * @return true if the order exists, false otherwise
     * @throws DomainOrderException if the check operation fails
     */
    boolean existsById(Long orderId) throws DomainOrderException;
}
