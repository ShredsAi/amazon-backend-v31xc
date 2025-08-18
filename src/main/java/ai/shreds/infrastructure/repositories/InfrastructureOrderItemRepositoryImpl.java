package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainEntityOrderItem;
import ai.shreds.infrastructure.exceptions.InfrastructureDatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public class InfrastructureOrderItemRepositoryImpl {

    private static final Logger log = LoggerFactory.getLogger(InfrastructureOrderItemRepositoryImpl.class);

    private final SpringDataOrderItemRepository orderItemRepository;

    @Autowired
    public InfrastructureOrderItemRepositoryImpl(SpringDataOrderItemRepository orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional
    public void saveItems(List<DomainEntityOrderItem> items) {
        try {
            log.debug("Saving {} order items", items.size());
            orderItemRepository.saveAll(items);
            log.info("Successfully saved {} order items", items.size());
        } catch (DataAccessException e) {
            log.error("Error saving order items: {}", e.getMessage());
            throw new InfrastructureDatabaseException("Failed to save order items", "SAVE_ITEMS", e);
        }
    }

    @Transactional(readOnly = true)
    public List<DomainEntityOrderItem> findByOrderId(Long orderId) {
        try {
            log.debug("Finding order items for order ID: {}", orderId);
            List<DomainEntityOrderItem> items = orderItemRepository.findByOrderId(orderId);
            log.debug("Found {} items for order ID: {}", items.size(), orderId);
            return items;
        } catch (DataAccessException e) {
            log.error("Error finding order items for order ID {}: {}", orderId, e.getMessage());
            throw new InfrastructureDatabaseException("Failed to find order items", "FIND_BY_ORDER_ID", e);
        }
    }

    @Transactional(readOnly = true)
    public List<DomainEntityOrderItem> findByOrderIdWithOrder(Long orderId) {
        try {
            log.debug("Finding order items with order details for order ID: {}", orderId);
            return orderItemRepository.findByOrderIdWithOrder(orderId);
        } catch (DataAccessException e) {
            log.error("Error finding order items with order for ID {}: {}", orderId, e.getMessage());
            throw new InfrastructureDatabaseException("Failed to find order items with order", "FIND_WITH_ORDER", e);
        }
    }

    @Transactional(readOnly = true)
    public Optional<DomainEntityOrderItem> findByOrderIdAndProductId(Long orderId, Long productId) {
        try {
            log.debug("Finding order item for order ID: {} and product ID: {}", orderId, productId);
            return orderItemRepository.findByOrderIdAndProductId(orderId, productId);
        } catch (DataAccessException e) {
            log.error("Error finding order item for order ID {} and product ID {}: {}", 
                    orderId, productId, e.getMessage());
            throw new InfrastructureDatabaseException("Failed to find order item", "FIND_BY_ORDER_AND_PRODUCT", e);
        }
    }

    @Transactional
    public void deleteByOrderId(Long orderId) {
        try {
            log.debug("Deleting order items for order ID: {}", orderId);
            orderItemRepository.deleteByOrderId(orderId);
            log.info("Successfully deleted order items for order ID: {}", orderId);
        } catch (DataAccessException e) {
            log.error("Error deleting order items for order ID {}: {}", orderId, e.getMessage());
            throw new InfrastructureDatabaseException("Failed to delete order items", "DELETE_BY_ORDER_ID", e);
        }
    }

    @Transactional(readOnly = true)
    public Integer getTotalQuantityByOrderId(Long orderId) {
        try {
            log.debug("Getting total quantity for order ID: {}", orderId);
            return orderItemRepository.getTotalQuantityByOrderId(orderId);
        } catch (DataAccessException e) {
            log.error("Error getting total quantity for order ID {}: {}", orderId, e.getMessage());
            throw new InfrastructureDatabaseException("Failed to get total quantity", "GET_TOTAL_QUANTITY", e);
        }
    }

    @Transactional(readOnly = true)
    public List<DomainEntityOrderItem> findByOrderIdIn(List<Long> orderIds) {
        try {
            log.debug("Finding order items for {} orders", orderIds.size());
            return orderItemRepository.findByOrderIdIn(orderIds);
        } catch (DataAccessException e) {
            log.error("Error finding order items for multiple orders: {}", e.getMessage());
            throw new InfrastructureDatabaseException("Failed to find order items for multiple orders", "FIND_BY_ORDER_IDS", e);
        }
    }

    @Transactional(readOnly = true)
    public boolean existsByOrderId(Long orderId) {
        try {
            log.debug("Checking existence of order items for order ID: {}", orderId);
            return orderItemRepository.existsByOrderId(orderId);
        } catch (DataAccessException e) {
            log.error("Error checking existence of order items for order ID {}: {}", orderId, e.getMessage());
            throw new InfrastructureDatabaseException("Failed to check order items existence", "EXISTS_BY_ORDER_ID", e);
        }
    }
}