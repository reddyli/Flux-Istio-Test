package li.reddy.order.repository;

import li.reddy.order.entity.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    @Query("SELECT o FROM Order o WHERE o.status = li.reddy.order.entity.OrderStatus.CONFIRMED " +
            "AND o.publishedAt IS NULL AND o.eventId IS NOT NULL ORDER BY o.createdAt ASC")
    List<Order> findUnpublishedConfirmed(Pageable pageable);
}
