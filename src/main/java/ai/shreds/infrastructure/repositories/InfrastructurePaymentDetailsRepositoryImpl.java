package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainEntityPaymentDetails;
import ai.shreds.domain.value_objects.DomainValuePaymentStatus;
import ai.shreds.infrastructure.exceptions.InfrastructureDatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class InfrastructurePaymentDetailsRepositoryImpl {

    private static final Logger log = LoggerFactory.getLogger(InfrastructurePaymentDetailsRepositoryImpl.class);

    private final SpringDataPaymentDetailsRepository paymentDetailsRepository;

    @Autowired
    public InfrastructurePaymentDetailsRepositoryImpl(SpringDataPaymentDetailsRepository paymentDetailsRepository) {
        this.paymentDetailsRepository = paymentDetailsRepository;
    }

    @Transactional
    public DomainEntityPaymentDetails save(DomainEntityPaymentDetails paymentDetails) {
        try {
            log.debug("Saving payment details for order ID: {}", paymentDetails.getOrderId());
            DomainEntityPaymentDetails saved = paymentDetailsRepository.save(paymentDetails);
            log.info("Successfully saved payment details with ID: {}", saved.getPaymentId());
            return saved;
        } catch (DataAccessException e) {
            log.error("Error saving payment details: {}", e.getMessage());
            throw new InfrastructureDatabaseException("Failed to save payment details", "SAVE_PAYMENT", e);
        }
    }

    @Transactional(readOnly = true)
    public Optional<DomainEntityPaymentDetails> findByPaymentId(String paymentId) {
        try {
            log.debug("Finding payment details by payment ID: {}", paymentId);
            return paymentDetailsRepository.findByPaymentId(paymentId);
        } catch (DataAccessException e) {
            log.error("Error finding payment details by payment ID {}: {}", paymentId, e.getMessage());
            throw new InfrastructureDatabaseException("Failed to find payment details", "FIND_BY_PAYMENT_ID", e);
        }
    }

    @Transactional(readOnly = true)
    public Optional<DomainEntityPaymentDetails> findByOrderId(Long orderId) {
        try {
            log.debug("Finding payment details by order ID: {}", orderId);
            return paymentDetailsRepository.findByOrderId(orderId);
        } catch (DataAccessException e) {
            log.error("Error finding payment details by order ID {}: {}", orderId, e.getMessage());
            throw new InfrastructureDatabaseException("Failed to find payment details", "FIND_BY_ORDER_ID", e);
        }
    }

    @Transactional(readOnly = true)
    public List<DomainEntityPaymentDetails> findByStatus(DomainValuePaymentStatus status) {
        try {
            log.debug("Finding payment details by status: {}", status);
            return paymentDetailsRepository.findByStatus(status);
        } catch (DataAccessException e) {
            log.error("Error finding payment details by status {}: {}", status, e.getMessage());
            throw new InfrastructureDatabaseException("Failed to find payment details by status", "FIND_BY_STATUS", e);
        }
    }

    @Transactional(readOnly = true)
    public List<DomainEntityPaymentDetails> findByOrderIdIn(List<Long> orderIds) {
        try {
            log.debug("Finding payment details for {} orders", orderIds.size());
            return paymentDetailsRepository.findByOrderIdIn(orderIds);
        } catch (DataAccessException e) {
            log.error("Error finding payment details for multiple orders: {}", e.getMessage());
            throw new InfrastructureDatabaseException("Failed to find payment details for orders", "FIND_BY_ORDER_IDS", e);
        }
    }

    @Transactional(readOnly = true)
    public List<DomainEntityPaymentDetails> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        try {
            log.debug("Finding payment details between {} and {}", startDate, endDate);
            return paymentDetailsRepository.findByDateRange(startDate, endDate);
        } catch (DataAccessException e) {
            log.error("Error finding payment details by date range: {}", e.getMessage());
            throw new InfrastructureDatabaseException("Failed to find payment details by date range", "FIND_BY_DATE_RANGE", e);
        }
    }

    @Transactional(readOnly = true)
    public List<DomainEntityPaymentDetails> findPendingPaymentsOlderThan(DomainValuePaymentStatus status, LocalDateTime cutoffDate) {
        try {
            log.debug("Finding pending payments older than {}", cutoffDate);
            return paymentDetailsRepository.findPendingPaymentsOlderThan(status, cutoffDate);
        } catch (DataAccessException e) {
            log.error("Error finding pending payments: {}", e.getMessage());
            throw new InfrastructureDatabaseException("Failed to find pending payments", "FIND_PENDING_PAYMENTS", e);
        }
    }

    @Transactional(readOnly = true)
    public boolean existsByOrderId(Long orderId) {
        try {
            log.debug("Checking existence of payment details for order ID: {}", orderId);
            return paymentDetailsRepository.existsByOrderId(orderId);
        } catch (DataAccessException e) {
            log.error("Error checking payment details existence for order ID {}: {}", orderId, e.getMessage());
            throw new InfrastructureDatabaseException("Failed to check payment details existence", "EXISTS_BY_ORDER_ID", e);
        }
    }

    @Transactional(readOnly = true)
    public List<DomainEntityPaymentDetails> findByPaymentMethod(String paymentMethod) {
        try {
            log.debug("Finding payment details by payment method: {}", paymentMethod);
            return paymentDetailsRepository.findByPaymentMethod(paymentMethod);
        } catch (DataAccessException e) {
            log.error("Error finding payment details by payment method {}: {}", paymentMethod, e.getMessage());
            throw new InfrastructureDatabaseException("Failed to find payment details by method", "FIND_BY_PAYMENT_METHOD", e);
        }
    }

    @Transactional(readOnly = true)
    public List<DomainEntityPaymentDetails> findRecentPaymentsByStatus(DomainValuePaymentStatus status, LocalDateTime cutoffDate) {
        try {
            log.debug("Finding recent payments by status {} after {}", status, cutoffDate);
            return paymentDetailsRepository.findRecentPaymentsByStatus(status, cutoffDate);
        } catch (DataAccessException e) {
            log.error("Error finding recent payments by status {}: {}", status, e.getMessage());
            throw new InfrastructureDatabaseException("Failed to find recent payments", "FIND_RECENT_PAYMENTS", e);
        }
    }

    @Transactional
    public void deleteByOrderId(Long orderId) {
        try {
            log.debug("Deleting payment details for order ID: {}", orderId);
            paymentDetailsRepository.deleteByOrderId(orderId);
            log.info("Successfully deleted payment details for order ID: {}", orderId);
        } catch (DataAccessException e) {
            log.error("Error deleting payment details for order ID {}: {}", orderId, e.getMessage());
            throw new InfrastructureDatabaseException("Failed to delete payment details", "DELETE_BY_ORDER_ID", e);
        }
    }
}