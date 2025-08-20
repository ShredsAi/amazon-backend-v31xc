package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainEntityOrder;
import ai.shreds.domain.entities.DomainEntityOrderItem;
import ai.shreds.domain.ports.DomainOutputPortOrderRepository;
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
public class InfrastructureOrderRepositoryImpl implements DomainOutputPortOrderRepository {

    private static final Logger log = LoggerFactory.getLogger(InfrastructureOrderRepositoryImpl.class);

    private final SpringDataOrderRepository orderRepository;
    private final SpringDataOrderItemRepository orderItemRepository;

    @Autowired
    public InfrastructureOrderRepositoryImpl(SpringDataOrderRepository orderRepository,
                                            SpringDataOrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    @Transactional
    public DomainEntityOrder save(DomainEntityOrder order) {
        try {
            log.debug("Saving order: {}", order);
            DomainEntityOrder savedOrder = orderRepository.save(order);
            log.info("Successfully saved order with ID: {}", savedOrder.getId());
            return savedOrder;
        } catch (DataAccessException e) {
            log.error("Error saving order: {}", e.getMessage());
            throw new InfrastructureDatabaseException("Failed to save order", "SAVE_ORDER", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DomainEntityOrder> findById(Long id) {
        try {
            log.debug("Finding order by ID: {}", id);
            Optional<DomainEntityOrder> order = orderRepository.findById(id);
            order.ifPresentOrElse(
                found -> log.debug("Found order: {}", found),
                () -> log.debug("Order not found with ID: {}", id)
            );
            return order;
        } catch (DataAccessException e) {
            log.error("Error finding order with ID {}: {}", id, e.getMessage());
            throw new InfrastructureDatabaseException("Failed to find order", "FIND_ORDER", e);
        }
    }

    @Override
    @Transactional
    public void saveItems(List<DomainEntityOrderItem> items) {
        try {
            log.debug("Saving {} order items", items.size());
            orderItemRepository.saveAll(items);
            log.info("Successfully saved {} order items", items.size());
        } catch (DataAccessException e) {
            log.error("Error saving order items: {}", e.getMessage());
            throw new InfrastructureDatabaseException("Failed to save order items", "SAVE_ORDER_ITEMS", e);
        }
    }

    @Override
    @Transactional
    public void deleteOrder(Long orderId) {
        try {
            log.debug("Deleting order with ID: {}", orderId);
            orderRepository.deleteById(orderId);
            log.info("Successfully deleted order with ID: {}", orderId);
        } catch (DataAccessException e) {
            log.error("Error deleting order with ID {}: {}", orderId, e.getMessage());
            throw new InfrastructureDatabaseException("Failed to delete order", "DELETE_ORDER", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DomainEntityOrderItem> findItemsByOrderId(Long orderId) {
        try {
            log.debug("Finding items for order ID: {}", orderId);
            List<DomainEntityOrderItem> items = orderItemRepository.findByOrderId(orderId);
            log.debug("Found {} items for order ID: {}", items.size(), orderId);
            return items;
        } catch (DataAccessException e) {
            log.error("Error finding items for order ID {}: {}", orderId, e.getMessage());
            throw new InfrastructureDatabaseException("Failed to find order items", "FIND_ORDER_ITEMS", e);
        }
    }

    @Override
    @Transactional
    public void updateItems(List<DomainEntityOrderItem> items) {
        try {
            log.debug("Updating {} order items", items.size());
            orderItemRepository.saveAll(items);
            log.info("Successfully updated {} order items", items.size());
        } catch (DataAccessException e) {
            log.error("Error updating order items: {}", e.getMessage());
            throw new InfrastructureDatabaseException("Failed to update order items", "UPDATE_ORDER_ITEMS", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long orderId) {
        try {
            log.debug("Checking existence of order with ID: {}", orderId);
            boolean exists = orderRepository.existsById(orderId);
            log.debug("Order with ID {} exists: {}", orderId, exists);
            return exists;
        } catch (DataAccessException e) {
            log.error("Error checking existence of order with ID {}: {}", orderId, e.getMessage());
            throw new InfrastructureDatabaseException("Failed to check order existence", "CHECK_ORDER_EXISTS", e);
        }
    }
}
