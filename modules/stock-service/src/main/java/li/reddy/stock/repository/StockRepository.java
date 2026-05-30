package li.reddy.stock.repository;

import li.reddy.stock.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock , String> {
}
