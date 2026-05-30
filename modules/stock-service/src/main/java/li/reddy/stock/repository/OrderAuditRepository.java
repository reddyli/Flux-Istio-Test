package li.reddy.stock.repository;

import li.reddy.stock.entity.OrderAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderAuditRepository extends JpaRepository<OrderAudit, UUID> {
}
