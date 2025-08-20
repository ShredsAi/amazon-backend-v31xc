package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainEntityOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataOrderRepository extends JpaRepository<DomainEntityOrder, Long> {
}
