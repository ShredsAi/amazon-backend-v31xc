package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainEntityOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataOrderItemRepository extends JpaRepository<DomainEntityOrderItem, Long> {

    @Query("SELECT oi FROM DomainEntityOrderItem oi WHERE oi.orderId = :orderId")
    List<DomainEntityOrderItem> findByOrderId(@Param("orderId") Long orderId);

    @Query("SELECT oi FROM DomainEntityOrderItem oi JOIN FETCH oi.order WHERE oi.orderId = :orderId")
    List<DomainEntityOrderItem> findByOrderIdWithOrder(@Param("orderId") Long orderId);

    @Query("SELECT oi FROM DomainEntityOrderItem oi WHERE oi.orderId = :orderId AND oi.productId = :productId")
    Optional<DomainEntityOrderItem> findByOrderIdAndProductId(
            @Param("orderId") Long orderId,
            @Param("productId") Long productId
    );

    @Query(value = "SELECT COUNT(oi) > 0 FROM DomainEntityOrderItem oi WHERE oi.orderId = :orderId")
    boolean existsByOrderId(@Param("orderId") Long orderId);

    @Query("DELETE FROM DomainEntityOrderItem oi WHERE oi.orderId = :orderId")
    void deleteByOrderId(@Param("orderId") Long orderId);

    @Query(value = "SELECT SUM(oi.quantity) FROM DomainEntityOrderItem oi WHERE oi.orderId = :orderId")
    Integer getTotalQuantityByOrderId(@Param("orderId") Long orderId);

    @Query(value = "SELECT oi FROM DomainEntityOrderItem oi WHERE oi.orderId IN :orderIds")
    List<DomainEntityOrderItem> findByOrderIdIn(@Param("orderIds") List<Long> orderIds);
}
