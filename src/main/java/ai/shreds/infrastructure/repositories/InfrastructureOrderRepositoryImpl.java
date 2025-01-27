package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainEntityOrder;
import ai.shreds.domain.entities.DomainEntityOrderItem;
import ai.shreds.domain.ports.DomainOutputPortOrderRepository;
import ai.shreds.infrastructure.exceptions.InfrastructureDatabaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
public class InfrastructureOrderRepositoryImpl implements DomainOutputPortOrderRepository {

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
            throw new InfrastructureDatabaseException("Failed to save order", e);
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
            throw new InfrastructureDatabaseException("Failed to find order", e);
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
            throw new InfrastructureDatabaseException("Failed to save order items", e);
        }
    }

    @Transactional
    public void deleteOrder(Long orderId) {
        try {
            log.debug("Deleting order with ID: {}", orderId);
            orderRepository.deleteById(orderId);
            log.info("Successfully deleted order with ID: {}", orderId);
        } catch (DataAccessException e) {
            log.error("Error deleting order with ID {}: {}", orderId, e.getMessage());
            throw new InfrastructureDatabaseException("Failed to delete order", e);
        }
    }

    @Transactional(readOnly = true)
    public List<DomainEntityOrderItem> findOrderItems(Long orderId) {
        try {
            log.debug("Finding items for order ID: {}", orderId);
            List<DomainEntityOrderItem> items = orderItemRepository.findByOrderId(orderId);
            log.debug("Found {} items for order ID: {}", items.size(), orderId);
            return items;
        } catch (DataAccessException e) {
            log.error("Error finding items for order ID {}: {}", orderId, e.getMessage());
            throw new InfrastructureDatabaseException("Failed to find order items", e);
        }
    }
}
