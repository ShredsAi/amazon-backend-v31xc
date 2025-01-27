package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainEntityPaymentDetails;
import ai.shreds.domain.value_objects.DomainValuePaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataPaymentDetailsRepository extends JpaRepository<DomainEntityPaymentDetails, String> {

    @Query("SELECT pd FROM DomainEntityPaymentDetails pd WHERE pd.paymentId = :paymentId")
    Optional<DomainEntityPaymentDetails> findByPaymentId(@Param("paymentId") String paymentId);

    @Query("SELECT pd FROM DomainEntityPaymentDetails pd WHERE pd.orderId = :orderId")
    Optional<DomainEntityPaymentDetails> findByOrderId(@Param("orderId") Long orderId);

    @Query("SELECT pd FROM DomainEntityPaymentDetails pd WHERE pd.status = :status")
    List<DomainEntityPaymentDetails> findByStatus(@Param("status") DomainValuePaymentStatus status);

    @Query("SELECT pd FROM DomainEntityPaymentDetails pd WHERE pd.orderId IN :orderIds")
    List<DomainEntityPaymentDetails> findByOrderIdIn(@Param("orderIds") List<Long> orderIds);

    @Query("SELECT pd FROM DomainEntityPaymentDetails pd WHERE pd.createdAt BETWEEN :startDate AND :endDate")
    List<DomainEntityPaymentDetails> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT pd FROM DomainEntityPaymentDetails pd WHERE pd.status = :status AND pd.createdAt < :cutoffDate")
    List<DomainEntityPaymentDetails> findPendingPaymentsOlderThan(
            @Param("status") DomainValuePaymentStatus status,
            @Param("cutoffDate") LocalDateTime cutoffDate
    );

    @Query("SELECT COUNT(pd) > 0 FROM DomainEntityPaymentDetails pd WHERE pd.orderId = :orderId")
    boolean existsByOrderId(@Param("orderId") Long orderId);

    @Query("SELECT pd FROM DomainEntityPaymentDetails pd WHERE pd.paymentMethod = :paymentMethod")
    List<DomainEntityPaymentDetails> findByPaymentMethod(@Param("paymentMethod") String paymentMethod);

    @Query(value = "SELECT pd FROM DomainEntityPaymentDetails pd WHERE pd.status = :status " +
            "AND pd.createdAt > :cutoffDate ORDER BY pd.createdAt DESC")
    List<DomainEntityPaymentDetails> findRecentPaymentsByStatus(
            @Param("status") DomainValuePaymentStatus status,
            @Param("cutoffDate") LocalDateTime cutoffDate
    );

    @Query("DELETE FROM DomainEntityPaymentDetails pd WHERE pd.orderId = :orderId")
    void deleteByOrderId(@Param("orderId") Long orderId);
}
