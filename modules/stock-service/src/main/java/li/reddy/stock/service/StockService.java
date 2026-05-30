package li.reddy.stock.service;


import org.springframework.transaction.annotation.Transactional;
import li.reddy.stock.entity.Stock;
import li.reddy.stock.exception.InsufficientStockException;
import li.reddy.stock.exception.UnknownSkuException;
import li.reddy.stock.repository.StockRepository;
import li.reddy.stock.web.StockResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class StockService {

    private final StockRepository stockRepository;
    private final StockCacheService stockCacheService;

    public StockService(StockRepository stockRepository, StockCacheService stockCacheService) {
        this.stockRepository = stockRepository;
        this.stockCacheService = stockCacheService;
    }

    @Transactional
    public void reserve(String sku, int quantity) {
        Stock stock = stockRepository.findById(sku)
                .orElseThrow(() -> new UnknownSkuException("Product does not exist: " + sku));
        if (stock.getQuantity() < quantity) {
            throw new InsufficientStockException("Insufficient stock quantity: " + quantity);
        }
        stock.setQuantity(stock.getQuantity() - quantity);
        stockRepository.save(stock);
        stockCacheService.invalidate(sku);
    }

    @Transactional
    public void replenish(String sku, int quantity) {
        Stock stock = stockRepository.findById(sku)
                .orElseThrow(() -> new UnknownSkuException("Product does not exist: " + sku));
        stock.setQuantity(stock.getQuantity() + quantity);
        stockRepository.save(stock);
        stockCacheService.invalidate(sku);
    }

    @Transactional(readOnly = true)
    public StockResponse getStock(String sku) {
        Optional<StockResponse> cached = stockCacheService.getStock(sku);
        if (cached.isPresent()) return cached.get();

        Stock stock = stockRepository.findById(sku)
                .orElseThrow(() -> new UnknownSkuException("Product does not exist: " + sku));

        StockResponse response = new StockResponse(stock.getSku(), stock.getQuantity());
        stockCacheService.putStock(sku, response);
        return response;
    }
}